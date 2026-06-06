package io.cargoiq.adapter.out.ai;

import io.cargoiq.domain.exception.ModelUnavailableException;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.ModelChoice;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link ChatModelRouter} — no Spring, no network. Verifies the
 * mock path is fully offline and that unavailable providers fail clearly.
 *
 * @author Vishal Dogra
 */
class ChatModelRouterTest {

    private final List<Citation> citations = List.of(
            new Citation(UUID.randomUUID(), UUID.randomUUID(),
                    "BL — Pacific Roasters", 0,
                    "Vessel MAERSK SYDNEY, port of discharge Brisbane.", 0.92),
            new Citation(UUID.randomUUID(), UUID.randomUUID(),
                    "Commercial Invoice", 1,
                    "Incoterm CIF, total USD 124,500.00.", 0.81));

    @Test
    void mockProviderAnswersOfflineFromCitations() {
        var router = router("mock");

        String answer = router.generateGrounded("What vessel?", citations, ModelChoice.mock());

        assertThat(answer)
                .contains("[#1]")
                .contains("MAERSK SYDNEY")
                .contains("Pacific Roasters")
                .containsIgnoringCase("mock");
    }

    @Test
    void nullChoiceFallsBackToServerDefault() {
        var router = router("mock");
        String answer = router.generateGrounded("anything", citations, new ModelChoice(null, null));
        assertThat(answer).contains("[#1]");
    }

    @Test
    void ollamaWithoutApiReportsUnavailable() {
        var router = router("mock"); // OllamaApi provider is empty in this test
        assertThatThrownBy(() ->
                router.generateGrounded("q", citations, new ModelChoice("ollama", "gemma2:9b")))
                .isInstanceOf(ModelUnavailableException.class)
                .hasMessageContaining("ollama");
    }

    @Test
    void configuredProviderWithoutBeanReportsUnavailable() {
        var router = router("mock");
        assertThatThrownBy(() ->
                router.generateGrounded("q", citations, new ModelChoice("openai", "gpt-4o-mini")))
                .isInstanceOf(ModelUnavailableException.class)
                .hasMessageContaining("openai");
    }

    private ChatModelRouter router(String serverDefault) {
        return new ChatModelRouter(empty(), empty(), serverDefault, "llama3.1");
    }

    /** An ObjectProvider that resolves to nothing. */
    private static <T> ObjectProvider<T> empty() {
        return new ObjectProvider<>() {
            @Override public T getObject() { throw new IllegalStateException("no bean"); }
            @Override public T getObject(Object... args) { throw new IllegalStateException("no bean"); }
            @Override public T getIfAvailable() { return null; }
            @Override public T getIfUnique() { return null; }
            @Override public Iterator<T> iterator() { return Collections.emptyIterator(); }
        };
    }
}
