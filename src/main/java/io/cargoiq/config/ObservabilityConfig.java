package io.cargoiq.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability defaults.
 *
 * <p>Tags every metric with {@code application} and {@code environment} so
 * Prometheus / Grafana queries can fan out cleanly across multiple deployments
 * of the same app.
 *
 * <p>Spring AI 1.1 ships its own micrometer integrations for LLM call latency
 * and token usage — they piggyback on this registry automatically.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String appName,
            @Value("${spring.profiles.active:default}") String environment) {
        return registry -> registry.config()
                .commonTags("application", appName, "environment", environment);
    }
}
