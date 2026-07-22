package io.cargoiq.adapter.in.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP <b>prompts</b> — reusable, named prompt templates an MCP client can list
 * and invoke with arguments to get a curated, expert starting point.
 *
 * <p>Each method is discovered automatically by Spring AI's MCP annotation
 * scanner (no manual registration) and surfaced at the MCP endpoint alongside
 * the {@code @Tool}s. The returned message is meant to be fed to a client LLM
 * that also has cargo-iq's tools ({@code search_cargo_documents},
 * {@code summarize_shipment}, {@code lookup_incoterm}) available — the prompt
 * tells it <i>how</i> to drive them for a real trade-finance task.
 *
 * @author Vishal Dogra
 */
@Component
public class TradeFinancePrompts {

    @McpPrompt(
            name = "compare_bl_to_invoice",
            title = "Compare Bill of Lading to Commercial Invoice",
            description = "Reconcile a Bill of Lading against a Commercial Invoice and surface "
                    + "every discrepancy (vessel, value, consignee, INCOTERM, quantities).")
    public GetPromptResult compareBlToInvoice(
            @McpArg(name = "blDocumentId", description = "Document ID of the Bill of Lading", required = true)
            String blDocumentId,
            @McpArg(name = "invoiceDocumentId", description = "Document ID of the Commercial Invoice", required = true)
            String invoiceDocumentId) {

        String text = """
                You are reconciling two trade documents in the cargo-iq corpus.

                1. Call `summarize_shipment` for the Bill of Lading (document id: %s).
                2. Call `summarize_shipment` for the Commercial Invoice (document id: %s).
                3. Produce a side-by-side comparison table covering: consignee/buyer,
                   shipper/seller, vessel, ports of loading and discharge, INCOTERM,
                   goods description, quantities, and total value/currency.
                4. Under a "Discrepancies" heading, list every field where the two
                   documents disagree or where one is silent. Flag anything a bank
                   would reject under UCP 600 (e.g. value or consignee mismatch).
                Cite the chunk indices you relied on.
                """.formatted(blDocumentId, invoiceDocumentId);

        return promptResult("Structured BL-vs-Invoice reconciliation", text);
    }

    @McpPrompt(
            name = "letter_of_credit_compliance_check",
            title = "Letter of Credit compliance check",
            description = "Enumerate the UCP 600 compliance points a bank would check when "
                    + "examining a Bill of Lading presented under a Letter of Credit.")
    public GetPromptResult letterOfCreditComplianceCheck(
            @McpArg(name = "blDocumentId", description = "Document ID of the Bill of Lading being presented", required = true)
            String blDocumentId,
            @McpArg(name = "lcDocumentId", description = "Document ID of the governing Letter of Credit", required = true)
            String lcDocumentId) {

        String text = """
                Act as a documentary-credit examiner applying UCP 600.

                1. Use `summarize_shipment` on the Letter of Credit (document id: %s) to
                   extract its terms: latest shipment date, expiry, described goods,
                   required documents, INCOTERM, amount and tolerance, partial-shipment
                   and transhipment allowances.
                2. Use `summarize_shipment` on the Bill of Lading (document id: %s).
                3. Examine the BL against the LC and produce a compliance checklist. For
                   each point mark COMPLIANT / DISCREPANT / CANNOT DETERMINE with a one-line
                   reason: on-board notation & date vs latest shipment date, consignee/
                   "to order" & endorsement, notify party, ports match, goods description
                   consistency, amount within tolerance, freight prepaid/collect vs INCOTERM.
                4. Conclude with an overall accept / reject-with-discrepancies recommendation.
                """.formatted(lcDocumentId, blDocumentId);

        return promptResult("UCP 600 compliance checklist", text);
    }

    @McpPrompt(
            name = "port_handover_brief",
            title = "Port handover / customs-clearance brief",
            description = "Brief an operations team on the documents and steps needed to clear "
                    + "customs at a given destination port.")
    public GetPromptResult portHandoverBrief(
            @McpArg(name = "port", description = "Destination port of discharge (e.g. Rotterdam, Brisbane)", required = true)
            String port,
            @McpArg(name = "incoterm", description = "Governing INCOTERM 2020 rule, if known (e.g. CIF, DAP)", required = false)
            String incoterm) {

        String incotermLine = (incoterm == null || incoterm.isBlank())
                ? "Determine the governing INCOTERM from the shipment documents."
                : "The governing INCOTERM is " + incoterm + " — call `lookup_incoterm` for its obligations.";

        String text = """
                Prepare a customs-clearance brief for the operations team handling cargo
                arriving at %s.

                1. %s
                2. Use `search_cargo_documents` to find shipments discharging at %s and list
                   them with their BL numbers and consignees.
                3. Enumerate the documents customs will require at %s (Bill of Lading,
                   Commercial Invoice, Packing List, Certificate of Origin, any import
                   licences) and note which are already in the corpus vs missing.
                4. Call out INCOTERM-driven responsibilities: who clears import, who pays
                   duty, where risk transfers.
                Keep it to a scannable checklist an ops coordinator can action.
                """.formatted(port, incotermLine, port, port);

        return promptResult("Customs-clearance brief for " + port, text);
    }

    // ---- helper ----

    private static GetPromptResult promptResult(String description, String text) {
        Content content = new TextContent(text);
        return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, content)));
    }
}
