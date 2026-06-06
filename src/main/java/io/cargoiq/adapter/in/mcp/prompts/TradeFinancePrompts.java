package io.cargoiq.adapter.in.mcp.prompts;

/**
 * Placeholder for MCP <b>prompts</b> — reusable, named prompt templates that
 * an MCP client can list and invoke (with arguments) to get a curated
 * starting point.
 *
 * <p>Registered via Spring AI 1.1's programmatic prompt support
 * (<code>McpServerFeatures.SyncPromptSpecification</code> / <code>@McpPrompt</code>).
 * Planned domain prompts:
 *
 * <ul>
 *   <li><b>compare_bl_to_invoice</b> — given two doc IDs, return a structured
 *       diff (vessel, value, consignee, INCOTERM consistency).</li>
 *   <li><b>letter_of_credit_compliance_check</b> — given a BL + LC pair,
 *       enumerate the UCP 600 compliance points a bank would check.</li>
 *   <li><b>port_handover_brief</b> — given a destination port, brief the
 *       operations team on documents needed for customs clearance there.</li>
 * </ul>
 *
 * <p>Tools are the headline MCP surface and ship first; prompts layer curated
 * starting points on top of them and are introduced once the tool inventory is
 * stable.
 */
public class TradeFinancePrompts {
    private TradeFinancePrompts() { }
}
