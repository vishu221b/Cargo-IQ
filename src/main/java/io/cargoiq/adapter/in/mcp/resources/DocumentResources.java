package io.cargoiq.adapter.in.mcp.resources;

/**
 * Placeholder for MCP <b>resources</b> — read-only, named pieces of context
 * (think: a list of file URIs the LLM client can fetch).
 *
 * <p><i>TODO</i> for you: expose each persisted {@link io.cargoiq.domain.model.Document}
 * as an MCP resource at URI <code>cargo://documents/{id}</code>, returning
 * the document's joined chunk text on read. Spring AI 1.1 supports both
 * fixed-URI and templated resources — see
 * {@code McpServerFeatures.SyncResourceSpecification} and
 * {@code McpServerFeatures.SyncResourceTemplateSpecification}.
 *
 * <p>Why this is a stub: in MCP, <b>tools</b> are for actions the LLM should
 * invoke and <b>resources</b> are for context the user or client app should
 * attach. The headline UX of this app is "LLM asks questions" (tools), not
 * "user attaches a document to the chat" (resources). Worth adding for
 * polish, not worth blocking on for v1.
 */
public class DocumentResources {
    private DocumentResources() { }
}
