package io.cargoiq.adapter.in.mcp.resources;

/**
 * Placeholder for MCP <b>resources</b> — read-only, named pieces of context
 * (think: a list of file URIs the LLM client can fetch).
 *
 * <p>Planned: expose each persisted {@link io.cargoiq.domain.model.Document}
 * as an MCP resource at URI <code>cargo://documents/{id}</code>, returning the
 * document's joined chunk text on read. Spring AI 1.1 supports both fixed-URI
 * and templated resources — see
 * {@code McpServerFeatures.SyncResourceSpecification} and
 * {@code McpServerFeatures.SyncResourceTemplateSpecification}.
 *
 * <p>In MCP, <b>tools</b> are for actions the LLM invokes and <b>resources</b>
 * are for context a user or client app attaches. The primary interaction here
 * is "LLM asks questions" (tools); document resources complement that for
 * clients that prefer to attach a known document to the conversation.
 */
public class DocumentResources {
    private DocumentResources() { }
}
