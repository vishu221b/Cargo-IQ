package io.cargoiq.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger metadata.
 *
 * <p>Declares the {@code bearer-jwt} security scheme so the spec (and the
 * Swagger UI "Authorize" button) document that protected endpoints expect an
 * {@code Authorization: Bearer <token>} header. The scheme is applied as a
 * global requirement; the genuinely public endpoints (auth, health, docs) still
 * work without a token — the requirement only tells clients how to authenticate.
 *
 * <p>The live, always-accurate spec is served at {@code /v3/api-docs} (JSON) and
 * {@code /v3/api-docs.yaml}; {@code scripts/export-openapi.sh} snapshots it to
 * {@code docs/openapi.json} for client/codegen use.
 *
 * @author Vishal Dogra
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI cargoIqOpenApi(@Value("${server.port:8080}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("cargo-iq API")
                        .description("""
                                RAG + MCP for international cargo, freight, and trade-finance
                                documents (Bills of Lading, Commercial Invoices, Letters of
                                Credit, INCOTERMS, HS codes). Ingest documents, then ask
                                grounded, citation-backed questions about them. The same
                                capabilities are exposed as an embedded MCP server at POST /mcp.

                                Authenticate at POST /api/v1/auth/login and send the returned
                                JWT as a bearer token. Corpus-mutating operations require the
                                ADMIN role; reads and queries require any authenticated user.""")
                        .version("v1")
                        .contact(new Contact().name("Vishal Dogra"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + port)
                        .description("Local")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtained from POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
