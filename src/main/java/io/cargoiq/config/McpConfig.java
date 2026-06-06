package io.cargoiq.config;

import io.cargoiq.adapter.in.mcp.tools.IngestDocumentTool;
import io.cargoiq.adapter.in.mcp.tools.LookupHsCodeTool;
import io.cargoiq.adapter.in.mcp.tools.LookupIncotermTool;
import io.cargoiq.adapter.in.mcp.tools.SearchDocumentsTool;
import io.cargoiq.adapter.in.mcp.tools.SummarizeShipmentTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires every {@code @Tool}-annotated bean into the MCP server.
 *
 * <p>The {@code spring-ai-starter-mcp-server-webmvc} auto-configuration picks
 * up any {@link ToolCallbackProvider} beans and exposes their tools at the
 * MCP transport endpoint. Listing the tool sources explicitly here (rather
 * than scanning the package) makes the inventory diff-reviewable — when
 * someone adds a new tool, this file is where they declare intent.
 *
 * <h3>Default endpoints (Streamable HTTP profile, see application.yml)</h3>
 * <ul>
 *   <li>POST {@code /mcp} — main protocol endpoint (Streamable HTTP transport)</li>
 *   <li>GET  {@code /actuator/info} — server metadata</li>
 * </ul>
 *
 * <p>If you switch the transport to SSE via {@code spring.ai.mcp.server.protocol},
 * the endpoints become {@code GET /sse} (subscribe) plus
 * {@code POST /mcp/messages} (commands).
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider cargoIqTools(
            SearchDocumentsTool searchDocumentsTool,
            LookupIncotermTool lookupIncotermTool,
            LookupHsCodeTool lookupHsCodeTool,
            SummarizeShipmentTool summarizeShipmentTool,
            IngestDocumentTool ingestDocumentTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        searchDocumentsTool,
                        lookupIncotermTool,
                        lookupHsCodeTool,
                        summarizeShipmentTool,
                        ingestDocumentTool)
                .build();
    }
}
