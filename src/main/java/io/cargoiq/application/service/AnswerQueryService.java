package io.cargoiq.application.service;

import io.cargoiq.application.port.in.AnswerQueryUseCase;
import io.cargoiq.application.port.out.ChatMemoryPort;
import io.cargoiq.application.port.out.ChatModelPort;
import io.cargoiq.application.port.out.KeywordSearchPort;
import io.cargoiq.application.port.out.QueryRewriterPort;
import io.cargoiq.application.port.out.RerankerPort;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.ConversationTurn;
import io.cargoiq.domain.model.Query;
import io.cargoiq.domain.model.RetrievalOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case: answer a natural-language query using RAG.
 *
 * <pre>
 *   Query
 *     │  (multi-query) rewrite into domain variants
 *     ▼
 *   for each variant:  dense (vector) + sparse (keyword) retrieval
 *     │
 *     ▼  Reciprocal Rank Fusion — one ranked, de-duplicated candidate list
 *     │
 *     ▼  (rerank) Maximal Marginal Relevance → top-k relevant & diverse
 *     │
 *     ▼  (memory) prepend recent conversation turns
 *     │
 *     ▼  ChatModelPort.generateGrounded → grounded answer text
 *     │
 *   Answer(text, citations)
 * </pre>
 *
 * <p>Every stage past baseline vector search is optional and dependency-free,
 * toggled per request via {@link RetrievalOptions}. Keeping the whole retrieval
 * strategy inside this one use case is deliberate — controllers and MCP tools
 * stay ignorant of how retrieval evolves.
 *
 * @author Vishal Dogra
 */
@Service
public class AnswerQueryService implements AnswerQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnswerQueryService.class);

    /** Retrieve this multiple of topK before fusion/rerank, so rerank has room to work. */
    private static final int OVERFETCH = 4;
    /** Reciprocal Rank Fusion damping constant (standard value). */
    private static final int RRF_K = 60;
    /** How many query variants to fan out to when multi-query is on. */
    private static final int MAX_VARIANTS = 3;
    /** Conversation turns (user+assistant) to prepend as memory. */
    private static final int MEMORY_TURNS = 6;

    private final VectorStorePort vectorStore;
    private final ChatModelPort chatModel;
    private final KeywordSearchPort keywordSearch;
    private final QueryRewriterPort queryRewriter;
    private final RerankerPort reranker;
    private final ChatMemoryPort chatMemory;

    /**
     * Full constructor used by Spring. {@code chatModel} is injected {@link Lazy}
     * to break a startup bean cycle: Spring AI's tool-calling autoconfiguration
     * makes the chat model depend on every {@code ToolCallbackProvider} bean —
     * including the MCP tool registry — whose tools depend back on this service.
     */
    @Autowired
    public AnswerQueryService(VectorStorePort vectorStore,
                              @Lazy ChatModelPort chatModel,
                              KeywordSearchPort keywordSearch,
                              QueryRewriterPort queryRewriter,
                              RerankerPort reranker,
                              ChatMemoryPort chatMemory) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.keywordSearch = keywordSearch;
        this.queryRewriter = queryRewriter;
        this.reranker = reranker;
        this.chatMemory = chatMemory;
    }

    /**
     * Convenience constructor for unit tests (and any caller that only cares
     * about baseline vector RAG): the extra retrieval stages degrade to no-ops.
     */
    public AnswerQueryService(VectorStorePort vectorStore, ChatModelPort chatModel) {
        this(vectorStore, chatModel,
                KeywordSearchPort.empty(),
                QueryRewriterPort.identity(),
                RerankerPort.identity(),
                ChatMemoryPort.noop());
    }

    @Override
    public Answer answer(Query query) {
        RetrievalOptions opts = query.retrieval();
        log.debug("RAG query: '{}' topK={} strategy=[{}] typeFilter={} conversation={}",
                query.text(), query.topK(), opts.label(), query.filterByType(), query.hasConversation());

        List<Citation> fused = retrieve(query, opts);

        if (fused.isEmpty()) {
            log.info("No retrieved context for query: '{}'", query.text());
            return new Answer(
                    "I couldn't find anything in the corpus that addresses that question. " +
                    "Try ingesting more documents, or rephrase your query.",
                    List.of());
        }

        List<Citation> top = opts.rerank()
                ? reranker.rerank(query.text(), fused, query.topK())
                : fused.stream().limit(query.topK()).toList();

        String userPrompt = withMemory(query, query.text());
        String answerText = chatModel.generateGrounded(userPrompt, top, query.chatOrMock());

        recordMemory(query, answerText);
        return new Answer(answerText, top);
    }

    // ---- retrieval: multi-query fan-out → dense + sparse → RRF fusion ----

    private List<Citation> retrieve(Query query, RetrievalOptions opts) {
        int candidateK = Math.min(50, Math.max(query.topK() * OVERFETCH, 12));

        List<String> variants = opts.multiQuery()
                ? queryRewriter.rewrite(query.text(), MAX_VARIANTS)
                : List.of(query.text());

        // Each ranked list contributes to fusion. Collect them, then fuse once.
        List<List<Citation>> rankedLists = new ArrayList<>();
        for (String variant : variants) {
            rankedLists.add(vectorStore.similaritySearch(new VectorStorePort.SearchRequest(
                    variant, candidateK, query.filterByType(), query.filterByIncoterm())));
            if (opts.hybrid()) {
                rankedLists.add(keywordSearch.keywordSearch(new KeywordSearchPort.SearchRequest(
                        variant, candidateK, query.filterByType(), query.filterByIncoterm())));
            }
        }

        // Single-list, no-fusion fast path preserves original scores/behaviour.
        if (rankedLists.size() == 1) {
            return rankedLists.get(0);
        }
        return reciprocalRankFusion(rankedLists, candidateK);
    }

    /**
     * Reciprocal Rank Fusion: combine several ranked lists into one by summing
     * {@code 1 / (RRF_K + rank)} per item across the lists it appears in, keyed
     * by chunk id. Rank-based, so it needs no score normalisation between the
     * (differently-scaled) dense and sparse lists.
     */
    private List<Citation> reciprocalRankFusion(List<List<Citation>> rankedLists, int limit) {
        Map<java.util.UUID, Citation> byChunk = new LinkedHashMap<>();
        Map<java.util.UUID, Double> fused = new LinkedHashMap<>();

        for (List<Citation> list : rankedLists) {
            for (int rank = 0; rank < list.size(); rank++) {
                Citation c = list.get(rank);
                byChunk.putIfAbsent(c.chunkId(), c);
                fused.merge(c.chunkId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            }
        }

        return fused.entrySet().stream()
                .sorted(Map.Entry.<java.util.UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> byChunk.get(e.getKey()))
                .toList();
    }

    // ---- conversational memory ----

    private String withMemory(Query query, String question) {
        if (!query.hasConversation()) return question;
        List<ConversationTurn> history = chatMemory.history(query.conversationId(), MEMORY_TURNS);
        if (history.isEmpty()) return question;

        StringBuilder sb = new StringBuilder("Conversation so far:\n");
        for (ConversationTurn t : history) {
            sb.append(t.role() == ConversationTurn.Role.USER ? "User: " : "Assistant: ")
              .append(t.text().strip()).append('\n');
        }
        sb.append("\nFollow-up question (resolve references against the conversation above): ")
          .append(question);
        return sb.toString();
    }

    private void recordMemory(Query query, String answerText) {
        if (!query.hasConversation()) return;
        chatMemory.append(query.conversationId(), ConversationTurn.user(query.text()));
        chatMemory.append(query.conversationId(), ConversationTurn.assistant(answerText));
    }
}
