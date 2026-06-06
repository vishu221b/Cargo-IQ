package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.ModelChoice;

import java.util.List;

/**
 * Outbound port: text generation grounded in retrieved citations.
 *
 * <p>Why this port exists rather than just injecting Spring AI's
 * {@code ChatClient} directly: prompts and grounding strategy are <i>business
 * logic</i>, not infrastructure. The system prompt — "answer only from the
 * provided context, cite source IDs, refuse if context is insufficient" — is
 * the heart of a RAG system. Hiding it behind a port means we can:
 *
 * <ul>
 *   <li>unit-test {@code AnswerQueryService} with a fake ChatModelPort;</li>
 *   <li>swap OpenAI → Anthropic → Ollama via configuration only;</li>
 *   <li>let the caller pick a provider/model per request (see {@link ModelChoice});</li>
 *   <li>evolve the prompt strategy without touching controllers or MCP tools.</li>
 * </ul>
 */
public interface ChatModelPort {

    /**
     * Generate a grounded answer.
     *
     * @param userQuery the question
     * @param context   retrieved citations to ground on
     * @param choice    which model to use; the adapter falls back to a
     *                  dependency-free mock when the choice is unavailable
     */
    String generateGrounded(String userQuery, List<Citation> context, ModelChoice choice);
}
