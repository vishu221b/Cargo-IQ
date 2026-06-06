package io.cargoiq.adapter.in.mcp.prompts;

/**
 * Placeholder for MCP <b>prompts</b> — reusable, named prompt templates that
 * an MCP client can list and invoke (with arguments) to get a curated
 * starting point.
 *
 * <p><i>TODO</i> for you to flesh out over the weekend: Spring AI 1.1 exposes
 * <code>@McpPrompt</code> / programmatic prompt registration via
 * <code>McpServerFeatures.SyncPromptSpecification</code>. Concrete prompt
 * candidates that would showcase the domain well:
 *
 * <ul>
 *   <li><b>compare_bl_to_invoice</b> — given two doc IDs, return a structured
 *       diff (vessel, value, consignee, INCOTERM consistency).</li>
 *   <li><b>letter_of_credit_compliance_check</b> — given a BL + LC pair,
 *       enumerate UCP 600 compliance points the bank would check.</li>
 *   <li><b>port_handover_brief</b> — given a destination port, brief the
 *       operations team on documents needed for customs clearance there.</li>
 * </ul>
 *
 * <p>The reason this is a stub rather than a working class: prompts are the
 * lowest-leverage MCP feature for a first release. Get tools working end-to-end
 * first, then circle back. Resources (next class) are similarly optional and
 * marked the same way.
 */
public class TradeFinancePrompts {
    private TradeFinancePrompts() { }
}
