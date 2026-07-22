package io.cargoiq.application.service;

import io.cargoiq.adapter.out.ai.HeuristicQueryRewriter;
import io.cargoiq.adapter.out.ai.MmrReranker;
import io.cargoiq.adapter.out.memory.InMemoryChatMemory;
import io.cargoiq.application.port.out.ChatMemoryPort;
import io.cargoiq.application.port.out.ChatModelPort;
import io.cargoiq.application.port.out.KeywordSearchPort;
import io.cargoiq.application.port.out.QueryRewriterPort;
import io.cargoiq.application.port.out.RerankerPort;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.DocumentChunk;
import io.cargoiq.domain.model.Query;
import io.cargoiq.domain.model.RetrievalOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ring-2 unit tests for the advanced retrieval pipeline in
 * {@link AnswerQueryService}: hybrid fusion, reranking, multi-query fan-out and
 * conversational memory — all driven against hand-rolled fakes plus the real
 * (pure, dependency-free) rewriter/reranker/memory adapters. No Spring.
 */
class AnswerQueryServiceRetrievalTest {

    private static Citation cite(String title, String snippet, double score) {
        return new Citation(UUID.randomUUID(), UUID.randomUUID(), title, 0, snippet, score);
    }

    @Test
    void hybridFusionMergesDenseAndSparseHitsWithoutDuplicates() {
        Citation shared = cite("BL-1", "vessel MAERSK SYDNEY discharge Brisbane", 0.9);
        Citation denseOnly = cite("BL-2", "port of loading Sydney", 0.5);
        Citation sparseOnly = cite("INV-9", "invoice value USD 42000", 0.4);

        var vector = vectorReturning(List.of(shared, denseOnly));
        KeywordSearchPort keyword = req -> List.of(shared, sparseOnly);

        var service = new AnswerQueryService(vector, echoChat(new AtomicReference<>()),
                keyword, QueryRewriterPort.identity(), RerankerPort.identity(), ChatMemoryPort.noop());

        Query q = new Query("brisbane", 10, Optional.empty(), Optional.empty(), null,
                new RetrievalOptions(true, false, false), null);
        Answer answer = service.answer(q);

        // shared appears once (dedup by chunkId), all three distinct chunks present.
        assertThat(answer.citations()).extracting(Citation::documentTitle)
                .containsExactlyInAnyOrder("BL-1", "BL-2", "INV-9");
    }

    @Test
    void rerankTruncatesToTopK() {
        var vector = vectorReturning(List.of(
                cite("A", "alpha", 0.9), cite("B", "bravo", 0.8),
                cite("C", "charlie", 0.7), cite("D", "delta", 0.6)));

        var service = new AnswerQueryService(vector, echoChat(new AtomicReference<>()),
                KeywordSearchPort.empty(), QueryRewriterPort.identity(), new MmrReranker(), ChatMemoryPort.noop());

        Query q = new Query("alpha bravo", 2, Optional.empty(), Optional.empty(), null,
                new RetrievalOptions(false, false, true), null);
        Answer answer = service.answer(q);

        assertThat(answer.citations()).hasSize(2);
    }

    @Test
    void multiQueryFansOutVariantsToTheRetriever() {
        AtomicReference<Integer> calls = new AtomicReference<>(0);
        VectorStorePort vector = new VectorStorePort() {
            @Override public void index(List<DocumentChunk> c, ChunkMetadata m) { }
            @Override public List<Citation> similaritySearch(SearchRequest r) {
                calls.updateAndGet(n -> n + 1);
                return List.of(cite("BL", "bill of lading text", 0.7));
            }
            @Override public void deleteByDocumentId(UUID d) { }
        };

        var service = new AnswerQueryService(vector, echoChat(new AtomicReference<>()),
                KeywordSearchPort.empty(), new HeuristicQueryRewriter(), RerankerPort.identity(), ChatMemoryPort.noop());

        // "bl" expands into extra variants, so the retriever is hit more than once.
        Query q = new Query("bl for brisbane", 6, Optional.empty(), Optional.empty(), null,
                new RetrievalOptions(false, true, false), null);
        service.answer(q);

        assertThat(calls.get()).isGreaterThan(1);
    }

    @Test
    void conversationMemoryFeedsPriorTurnsIntoTheNextPrompt() {
        var memory = new InMemoryChatMemory();
        var vector = vectorReturning(List.of(cite("BL", "vessel MAERSK SYDNEY", 0.9)));
        AtomicReference<String> lastPrompt = new AtomicReference<>();

        var service = new AnswerQueryService(vector, echoChat(lastPrompt),
                KeywordSearchPort.empty(), QueryRewriterPort.identity(), RerankerPort.identity(), memory);

        String convo = "conv-123";
        service.answer(new Query("What vessel?", 6, Optional.empty(), Optional.empty(), null,
                RetrievalOptions.plain(), convo));
        service.answer(new Query("And its discharge port?", 6, Optional.empty(), Optional.empty(), null,
                RetrievalOptions.plain(), convo));

        // The second prompt carries the first exchange as memory.
        assertThat(lastPrompt.get()).contains("Conversation so far").contains("What vessel?");
        assertThat(memory.history(convo, 10)).hasSize(4); // 2 user + 2 assistant turns
    }

    // ---- fakes ----

    private static VectorStorePort vectorReturning(List<Citation> results) {
        return new VectorStorePort() {
            @Override public void index(List<DocumentChunk> c, ChunkMetadata m) { }
            @Override public List<Citation> similaritySearch(SearchRequest r) { return results; }
            @Override public void deleteByDocumentId(UUID d) { }
        };
    }

    /** A chat port that records the prompt it received and echoes a fixed reply. */
    private static ChatModelPort echoChat(AtomicReference<String> sink) {
        return (q, ctx, choice) -> {
            sink.set(q);
            return "ok";
        };
    }
}
