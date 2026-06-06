package io.cargoiq.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI-related configuration.
 *
 * <p>Intentionally minimal. Spring AI's auto-configuration (driven by the
 * {@code spring-ai-starter-model-openai} and
 * {@code spring-ai-starter-vector-store-pgvector} starters in {@code pom.xml})
 * produces the following beans for us:
 *
 * <ul>
 *   <li>{@code ChatModel}            — backed by {@code OpenAiChatModel}</li>
 *   <li>{@code EmbeddingModel}       — backed by {@code OpenAiEmbeddingModel}</li>
 *   <li>{@code VectorStore}          — backed by {@code PgVectorStore}</li>
 *   <li>{@code ChatClient.Builder}   — for {@link io.cargoiq.adapter.out.ai.SpringAiChatModelAdapter}</li>
 * </ul>
 *
 * <p>All driven by properties in {@code application.yml}. To swap to Anthropic
 * or Ollama, change the starter dependency and the relevant properties —
 * nothing in this file changes.
 */
@Configuration
public class AiConfig {
    // Reserved for future bean overrides — e.g. custom retry advisor,
    // token-usage logger, etc.
}
