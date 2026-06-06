package io.cargoiq.application.service;

import io.cargoiq.application.port.in.AnswerQueryUseCase;
import io.cargoiq.application.port.out.ChatModelPort;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case: answer a natural-language query using RAG.
 *
 * <pre>
 *   Query
 *     │
 *     ▼  VectorStorePort.similaritySearch     (retrieve)
 *   List&lt;Citation&gt;
 *     │
 *     ▼  ChatModelPort.generateGrounded       (augment + generate)
 *   String answer text
 *     │
 *     ▼  combine → Answer (text + citations)
 * </pre>
 *
 * <p>The retrieval-then-generation split is what makes RAG <i>RAG</i> rather
 * than a chat over your own data. Keeping both steps inside a single use case
 * is deliberate: it lets us evolve the retrieval strategy (re-ranking, HyDE,
 * multi-query) without leaking detail into controllers or MCP tools.
 *
 * <h3>Where to extend</h3>
 * <ul>
 *   <li><b>Re-ranking:</b> after similaritySearch, call a cross-encoder
 *       re-ranker (e.g. Cohere Rerank, or a local bge-reranker via Ollama).</li>
 *   <li><b>Multi-query rewriting:</b> have the ChatModelPort produce 3 query
 *       variants, retrieve for each, dedupe by chunkId, then generate.</li>
 *   <li><b>Conversational memory:</b> add a {@code ChatMemoryPort} and prepend
 *       prior turns. Spring AI's ChatMemory primitives can sit behind it.</li>
 * </ul>
 */
@Service
public class AnswerQueryService implements AnswerQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnswerQueryService.class);

    private final VectorStorePort vectorStore;
    private final ChatModelPort chatModel;

    public AnswerQueryService(VectorStorePort vectorStore, ChatModelPort chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    @Override
    public Answer answer(Query query) {
        log.debug("RAG query: '{}' topK={} typeFilter={} incotermFilter={}",
                query.text(), query.topK(), query.filterByType(), query.filterByIncoterm());

        List<Citation> citations = vectorStore.similaritySearch(
                new VectorStorePort.SearchRequest(
                        query.text(),
                        query.topK(),
                        query.filterByType(),
                        query.filterByIncoterm()));

        if (citations.isEmpty()) {
            log.info("No retrieved context for query: '{}'", query.text());
            return new Answer(
                    "I couldn't find anything in the corpus that addresses that question. " +
                    "Try ingesting more documents, or rephrase your query.",
                    List.of());
        }

        String answerText = chatModel.generateGrounded(query.text(), citations);
        return new Answer(answerText, citations);
    }
}
