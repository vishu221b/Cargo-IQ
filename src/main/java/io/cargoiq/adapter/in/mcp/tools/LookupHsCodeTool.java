package io.cargoiq.adapter.in.mcp.tools;

import io.cargoiq.application.port.in.LookupHsCodeUseCase;
import io.cargoiq.domain.model.HsCode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LookupHsCodeTool {

    private final LookupHsCodeUseCase lookupHsCode;

    public LookupHsCodeTool(LookupHsCodeUseCase lookupHsCode) {
        this.lookupHsCode = lookupHsCode;
    }

    @Tool(
        name = "lookup_hs_code",
        description = """
            Look up a Harmonised System (HS) tariff code by exact code. The HS
            code is the 6-digit (or longer) classification customs authorities
            use worldwide. Returns the code, description, and chapter.
            """
    )
    public HsCode byCode(
            @ToolParam(description = "HS code, 4-10 digits, e.g. '851712'") String code) {
        return lookupHsCode.byCode(code);
    }

    @Tool(
        name = "search_hs_codes",
        description = """
            Search the HS tariff schedule by free-text description. Returns up
            to 'limit' matches, ranked by description similarity. Use when the
            user describes a product but doesn't know the code.
            """
    )
    public List<HsCode> search(
            @ToolParam(description = "Free-text description of the goods") String description,
            @ToolParam(description = "Max results, default 10") Integer limit) {
        return lookupHsCode.search(description, limit != null ? limit : 10);
    }
}
