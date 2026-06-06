package io.cargoiq.adapter.out.ai;

import io.cargoiq.application.port.out.ChatModelPort;
import io.cargoiq.domain.model.Citation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Adapter that implements {@link ChatModelPort} using Spring AI's
 * {@link ChatClient}.
 *
 * <p>The system prompt below is the RAG safety net. It instructs the model
 * to answer <i>only</i> from the supplied context and to refuse otherwise.
 * Combined with citation echoes in the user prompt, this gets us most of the
 * way to grounded answers. Without this prompt the model will happily
 * hallucinate INCOTERMS rules that don't exist.
 *
 * <p>If you swap models (Anthropic, Ollama, etc.) you change <i>configuration
 * properties</i> only — Spring AI's {@code ChatClient} abstracts the provider.
 * That's the win of putting the prompt strategy here and not in the
 * application service.
 */
@Component
public class SpringAiChatModelAdapter implements ChatModelPort {

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

    private final ChatClient chatClient;

    public SpringAiChatModelAdapter(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public String generateGrounded(String userQuery, List<Citation> context) {
        String contextBlock = IntStream.range(0, context.size())
                .mapToObj(i -> formatCitation(i + 1, context.get(i)))
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        String userPrompt = """
                Context (cite by [#N]):
                %s

                ---
                Question: %s
                """.formatted(contextBlock, userQuery);

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    private String formatCitation(int index, Citation c) {
        return "[#%d] %s (chunk %d, doc %s)\n%s".formatted(
                index,
                c.documentTitle(),
                c.chunkSequence(),
                c.documentId(),
                c.snippet());
    }
}
