package io.cargoiq.application.service;

import io.cargoiq.application.port.out.ChatModelPort;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link AnswerQueryService} using hand-rolled fake ports.
 *
 * <p>This is the payoff of hexagonal architecture: zero Spring, zero
 * Testcontainers, zero network — the service is exercised in pure Java
 * against in-memory fakes that satisfy its outbound ports. Sub-millisecond.
 *
 * <p>The same pattern works for every other use case in the codebase. When
 * you're filling in business rules over the weekend, write the test first
 * with a fake, then drop in the real adapter and integration-test it
 * separately.
 */
class AnswerQueryServiceTest {

    @Test
    void returnsUngroundedFallbackWhenNoCitations() {
        var service = new AnswerQueryService(emptyVectorStore(), chatThatShouldNotBeCalled());

        Answer answer = service.answer(Query.of("Anything?"));

        assertThat(answer.isGrounded()).isFalse();
        assertThat(answer.citations()).isEmpty();
        assertThat(answer.text()).contains("couldn't find anything");
    }

    @Test
    void synthesisesAnswerWhenCitationsAreReturned() {
        var fakeCitation = new Citation(
                UUID.randomUUID(), UUID.randomUUID(),
                "BL-12345", 0,
                "Vessel MAERSK SYDNEY, port of discharge Brisbane.",
                0.91);
        var service = new AnswerQueryService(
                vectorStoreReturning(fakeCitation),
                chatReturning("Vessel: MAERSK SYDNEY [#1]."));

        Answer answer = service.answer(Query.of("What vessel?"));

        assertThat(answer.isGrounded()).isTrue();
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.text()).contains("MAERSK SYDNEY");
    }

    // ---- fakes ----

    private static VectorStorePort emptyVectorStore() {
        return new VectorStorePort() {
            @Override public void index(List<io.cargoiq.domain.model.DocumentChunk> c, ChunkMetadata m) { }
            @Override public List<Citation> similaritySearch(SearchRequest r) { return List.of(); }
            @Override public void deleteByDocumentId(UUID d) { }
        };
    }

    private static VectorStorePort vectorStoreReturning(Citation... results) {
        return new VectorStorePort() {
            @Override public void index(List<io.cargoiq.domain.model.DocumentChunk> c, ChunkMetadata m) { }
            @Override public List<Citation> similaritySearch(SearchRequest r) { return List.of(results); }
            @Override public void deleteByDocumentId(UUID d) { }
        };
    }

    private static ChatModelPort chatReturning(String reply) {
        return (q, ctx) -> reply;
    }

    private static ChatModelPort chatThatShouldNotBeCalled() {
        return (q, ctx) -> {
            throw new AssertionError("ChatModelPort should not be called when no citations");
        };
    }
}
