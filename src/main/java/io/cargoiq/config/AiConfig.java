package io.cargoiq.config;

import io.cargoiq.adapter.out.ai.MockEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI-related configuration.
 *
 * <p>The vector store, MCP server, and (optionally) a real chat/embedding model
 * are produced by Spring AI auto-configuration driven by {@code application.yml}
 * — see {@code spring.ai.model.chat} / {@code spring.ai.model.embedding}.
 *
 * <p>The one bean defined here is the fallback {@link MockEmbeddingModel}: when
 * no real embedding provider is selected (the default, so the app runs with no
 * API key), it satisfies the {@code EmbeddingModel} dependency of the pgvector
 * store with deterministic, lexically-meaningful vectors. {@code @ConditionalOnMissingBean}
 * means a configured provider (OpenAI/Ollama/Gemini) transparently replaces it.
 *
 * <p>The chat side is handled by {@link io.cargoiq.adapter.out.ai.ChatModelRouter},
 * which picks mock / Ollama / a configured provider per request.
 */
@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel mockEmbeddingModel(
            @Value("${spring.ai.vectorstore.pgvector.dimensions:1536}") int dimensions) {
        return new MockEmbeddingModel(dimensions);
    }
}
