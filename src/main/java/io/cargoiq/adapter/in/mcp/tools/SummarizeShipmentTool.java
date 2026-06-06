package io.cargoiq.adapter.in.mcp.tools;

import io.cargoiq.application.port.in.AnswerQueryUseCase;
import io.cargoiq.application.port.in.ListDocumentsUseCase;
import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Query;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MCP tool — summarise a specific document, given its ID.
 *
 * <p>Two-step internally: load the doc title for grounding context, then call
 * the RAG use case with a tailored prompt. Demonstrates composing multiple
 * use cases inside a single MCP tool — fine, because it's still an adapter
 * orchestration concern, not a domain rule.
 */
@Component
public class SummarizeShipmentTool {

    private final ListDocumentsUseCase listDocs;
    private final AnswerQueryUseCase answerQuery;

    public SummarizeShipmentTool(ListDocumentsUseCase listDocs, AnswerQueryUseCase answerQuery) {
        this.listDocs = listDocs;
        this.answerQuery = answerQuery;
    }

    @Tool(
        name = "summarize_shipment",
        description = """
            Produce a structured summary of a single shipping document
            (Bill of Lading, Commercial Invoice, etc.) given its document ID.
            Covers parties, vessel, ports, INCOTERM, value, and any noted
            discrepancies. Use this when the user asks for a recap of a known
            document — not for free-form search across the corpus.
            """
    )
    public Answer summarize(
            @ToolParam(description = "The document UUID to summarise") String documentId) {
        var doc = listDocs.byId(UUID.fromString(documentId));
        String prompt = "Summarise the shipping document titled \"" + doc.title()
                + "\". Cover parties, vessel and ports, INCOTERM, value, and any "
                + "anomalies. Structure as bullets.";
        return answerQuery.answer(new Query(prompt, 10,
                java.util.Optional.empty(), java.util.Optional.empty()));
    }
}
