package io.cargoiq.adapter.in.mcp.tools;

import io.cargoiq.application.port.in.LookupIncotermUseCase;
import io.cargoiq.application.port.in.LookupIncotermUseCase.IncotermDetail;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tool — deterministic lookup of an INCOTERMS 2020 rule.
 *
 * <p>Useful complement to {@link SearchDocumentsTool}: that one does RAG,
 * which can hallucinate; this one returns canonical reference data. The LLM
 * agent will typically prefer the deterministic tool for rule lookups and
 * the RAG tool for queries about <i>specific shipments</i>.
 */
@Component
public class LookupIncotermTool {

    private final LookupIncotermUseCase lookupIncoterm;

    public LookupIncotermTool(LookupIncotermUseCase lookupIncoterm) {
        this.lookupIncoterm = lookupIncoterm;
    }

    @Tool(
        name = "lookup_incoterm",
        description = """
            Return the canonical INCOTERMS 2020 definition for a given three-letter
            rule code (EXW, FCA, CPT, CIP, DAP, DPU, DDP, FAS, FOB, CFR, CIF).
            Output includes summary, seller and buyer obligations, the risk and
            cost transfer points, and a typical use case. Use this whenever the
            user asks what a specific INCOTERM means, who pays for what, or where
            risk transfers — never use the document-search tool for that.
            """
    )
    public IncotermDetail lookup(
            @ToolParam(description = "Three-letter INCOTERM 2020 code, e.g. CIF")
            String code) {
        return lookupIncoterm.lookup(code);
    }
}
