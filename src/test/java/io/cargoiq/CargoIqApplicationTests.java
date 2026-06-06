package io.cargoiq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Context-load smoke test.
 *
 * <p>Catches 80% of misconfiguration before it hits a running server: bean
 * wiring, port-to-adapter resolution, properties resolution, Flyway migration
 * validity. If this test passes, the application can at least start.
 *
 * <p>Boots a real pgvector-enabled Postgres via Testcontainers — sharing the
 * production schema initialisation path. The OpenAI key is stubbed; LLM calls
 * won't run, but no LLM call happens at startup either, so we never actually
 * hit OpenAI in this test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CargoIqApplicationTests {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName
                    .parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("cargoiq")
                    .withUsername("cargoiq")
                    .withPassword("cargoiq");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Stub the OpenAI key so the autoconfig doesn't refuse to start.
        // No real LLM call happens during context load.
        registry.add("spring.ai.openai.api-key", () -> "test-key-not-real");
    }

    @Test
    void contextLoads() {
        // Empty body. The @SpringBootTest annotation does the work — if any
        // bean fails to wire, the test fails. That's the entire point.
    }
}
