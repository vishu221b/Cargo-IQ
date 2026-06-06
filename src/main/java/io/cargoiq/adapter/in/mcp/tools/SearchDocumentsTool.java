package io.cargoiq.adapter.in.mcp.tools;

import io.cargoiq.application.port.in.AnswerQueryUseCase;
import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import io.cargoiq.domain.model.Query;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * MCP tool — semantic search + RAG answer over the ingested cargo corpus.
 *
 * <p>This is the headline tool. When an LLM client (Claude Desktop, Cursor)
 * connects to the MCP server, it discovers this tool by name and description
 * and learns it can ask grounded questions about shipping documents.
 *
 * <p>The tool method body is one line: delegate to the use case. <i>That is
 * intentional.</i> An MCP tool is an inbound adapter, exactly like a REST
 * controller. Identical structural role.
 */
@Component
public class SearchDocumentsTool {

    private final AnswerQueryUseCase answerQuery;

    public SearchDocumentsTool(AnswerQueryUseCase answerQuery) {
        this.answerQuery = answerQuery;
    }

    @Tool(
        name = "search_cargo_documents",
        description = """
            Search and answer questions about ingested cargo and trade-finance
            documents (Bills of Lading, Commercial Invoices, Letters of Credit,
            Charter Parties, INCOTERMS reference). Uses RAG: retrieves the most
            relevant text chunks, then synthesises a grounded answer with
            citations. Use this when the user asks anything about specific
            shipments, contract terms, or trade-finance practice that is
            answerable from the corpus.
            """
    )
    public Answer search(
            @ToolParam(description = "The user's natural-language question")
            String question,

            @ToolParam(description = "Number of chunks to retrieve, 1-50; default 6")
            Integer topK,

            @ToolParam(description = "Optional filter: only documents of this type "
                    + "(BILL_OF_LADING, COMMERCIAL_INVOICE, LETTER_OF_CREDIT, ...)")
            DocumentType filterByType,

            @ToolParam(description = "Optional filter: only chunks from documents tagged "
                    + "with this INCOTERM 2020 code (EXW, FCA, CPT, CIP, DAP, DPU, DDP, "
                    + "FAS, FOB, CFR, CIF)")
            Incoterm filterByIncoterm
    ) {
        int k = topK != null ? topK : 6;
        return answerQuery.answer(new Query(
                question, k,
                Optional.ofNullable(filterByType),
                Optional.ofNullable(filterByIncoterm)));
    }
}
