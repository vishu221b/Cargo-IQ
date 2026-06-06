package io.cargoiq.adapter.out.ai;

import io.cargoiq.application.port.out.ChatModelPort;
import io.cargoiq.domain.exception.ModelUnavailableException;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.ModelChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * {@link ChatModelPort} that routes a request to a chat model chosen at runtime.
 *
 * <p>Three answering paths:
 * <ul>
 *   <li><b>mock</b> (default) — no external dependency. Synthesises a grounded
 *       answer directly from the retrieved citations so the whole RAG pipeline
 *       is testable locally with <i>no API key and no model server</i>.</li>
 *   <li><b>ollama</b> — builds an Ollama chat model on demand for any pulled
 *       model name (e.g. {@code gemma2:9b}); the {@code OllamaApi} bean is always
 *       on the classpath, so this needs only a local Ollama server.</li>
 *   <li><b>configured provider</b> — if the server was started with a real
 *       provider ({@code AI_CHAT_PROVIDER=openai|anthropic|google-genai} + key),
 *       that {@link ChatModel} bean is used.</li>
 * </ul>
 *
 * <p>The system prompt — the grounding contract — lives here: it is business
 * logic, not infrastructure. Spring AI beans are looked up via
 * {@link ObjectProvider} (never a hard constructor dependency), which also keeps
 * this off the MCP-tool ↔ chat-model startup cycle.
 *
 * @author Vishal Dogra
 */
@Component
public class ChatModelRouter implements ChatModelPort {

    private static final Logger log = LoggerFactory.getLogger(ChatModelRouter.class);

    private static final String SYSTEM_PROMPT = """
            You are cargo-iq, an assistant for international cargo, freight, and
            trade-finance professionals. Answer questions using ONLY the context
            chunks supplied in the user message.

            Rules:
              1. If the context does not contain enough information, say so
                 explicitly. Do NOT invent details — especially not vessel names,
                 BL numbers, monetary values, or INCOTERM rule mechanics.
              2. When you cite a fact, reference the chunk by its [#N] index
                 from the supplied context.
              3. Be terse and structured. Bullets are preferred over prose for
                 multi-fact answers. Use the canonical trade-finance vocabulary
                 (consignee, port of discharge, BL, LC, demurrage, etc.).
              4. If asked to compare or reconcile documents, explicitly call out
                 discrepancies — that is the highest-value use of this tool.
            """;

    private final ObjectProvider<OllamaApi> ollamaApi;
    private final ObjectProvider<ChatModel> configuredChatModel;
    private final String serverDefaultProvider;
    private final String defaultOllamaModel;
    private final Map<String, OllamaChatModel> ollamaModels = new ConcurrentHashMap<>();

    public ChatModelRouter(
            ObjectProvider<OllamaApi> ollamaApi,
            ObjectProvider<ChatModel> configuredChatModel,
            @Value("${spring.ai.model.chat:mock}") String serverDefaultProvider,
            @Value("${spring.ai.ollama.chat.options.model:llama3.1}") String defaultOllamaModel) {
        this.ollamaApi = ollamaApi;
        this.configuredChatModel = configuredChatModel;
        this.serverDefaultProvider = serverDefaultProvider;
        this.defaultOllamaModel = defaultOllamaModel;
    }

    @Override
    public String generateGrounded(String userQuery, List<Citation> context, ModelChoice choice) {
        String provider = choice.hasProvider() ? choice.providerId() : serverDefaultProvider;
        String userPrompt = buildUserPrompt(userQuery, context);

        return switch (provider) {
            case "", "mock", "none" -> mockAnswer(userQuery, context);
            case "ollama" -> ollamaAnswer(choice, userPrompt);
            default -> configuredAnswer(provider, userPrompt);
        };
    }

    // ---- ollama (runtime model selection) ----

    private String ollamaAnswer(ModelChoice choice, String userPrompt) {
        OllamaApi api = ollamaApi.getIfAvailable();
        if (api == null) {
            throw new ModelUnavailableException("ollama", "the Ollama integration is not on the classpath");
        }
        String model = choice.hasModel() ? choice.model().trim() : defaultOllamaModel;
        try {
            OllamaChatModel chat = ollamaModels.computeIfAbsent(model, m ->
                    OllamaChatModel.builder()
                            .ollamaApi(api)
                            .defaultOptions(OllamaChatOptions.builder().model(m).temperature(0.1).build())
                            .build());
            return ChatClient.create(chat)
                    .prompt().system(SYSTEM_PROMPT).user(userPrompt).call().content();
        } catch (Exception e) {
            log.warn("Ollama call failed for model '{}': {}", model, e.getMessage());
            throw new ModelUnavailableException("ollama:" + model,
                    "is Ollama running and the model pulled? (" + e.getMessage() + ")");
        }
    }

    // ---- a server-configured Spring AI provider ----

    private String configuredAnswer(String provider, String userPrompt) {
        ChatModel model = configuredChatModel.getIfAvailable();
        if (model == null) {
            throw new ModelUnavailableException(provider,
                    "no model is configured on the server for this provider — start it with "
                    + "AI_CHAT_PROVIDER=" + provider + " and the matching API key, or use 'mock'/'ollama'");
        }
        try {
            return ChatClient.create(model)
                    .prompt().system(SYSTEM_PROMPT).user(userPrompt).call().content();
        } catch (Exception e) {
            log.warn("Configured chat model call failed: {}", e.getMessage());
            throw new ModelUnavailableException(provider, e.getMessage());
        }
    }

    // ---- mock (no external dependency) ----

    private String mockAnswer(String userQuery, List<Citation> context) {
        var sb = new StringBuilder();
        sb.append("Based on ").append(context.size())
          .append(context.size() == 1 ? " passage" : " passages")
          .append(" retrieved from the corpus:\n\n");
        int shown = Math.min(context.size(), 5);
        for (int i = 0; i < shown; i++) {
            Citation c = context.get(i);
            sb.append("• [#").append(i + 1).append("] ")
              .append(firstSentences(c.snippet(), 240))
              .append("  — _").append(c.documentTitle()).append("_\n");
        }
        sb.append("\n_Answered by the built-in mock model (no LLM call). "
                + "Pick a provider — Ollama with a local model, or a configured "
                + "OpenAI/Gemini key — for a synthesised answer._");
        return sb.toString();
    }

    // ---- helpers ----

    private String buildUserPrompt(String userQuery, List<Citation> context) {
        String contextBlock = IntStream.range(0, context.size())
                .mapToObj(i -> formatCitation(i + 1, context.get(i)))
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
        return """
                Context (cite by [#N]):
                %s

                ---
                Question: %s
                """.formatted(contextBlock, userQuery);
    }

    private String formatCitation(int index, Citation c) {
        return "[#%d] %s (chunk %d, doc %s)\n%s".formatted(
                index, c.documentTitle(), c.chunkSequence(), c.documentId(), c.snippet());
    }

    private static String firstSentences(String text, int max) {
        String trimmed = text.strip().replaceAll("\\s+", " ");
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max).trim() + "…";
    }
}
