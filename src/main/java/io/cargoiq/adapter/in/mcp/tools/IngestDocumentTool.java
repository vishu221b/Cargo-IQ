package io.cargoiq.adapter.in.mcp.tools;

import io.cargoiq.application.port.in.IngestDocumentUseCase;
import io.cargoiq.application.port.in.IngestDocumentUseCase.IngestCommand;
import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tool — ingest a new document into the corpus.
 *
 * <p>Lets an LLM client (or an agentic workflow) push documents straight into
 * the RAG store. Useful for demos like "Claude, here's the new BL — index it
 * and then tell me how it compares to the previous one for this consignee."
 */
@Component
public class IngestDocumentTool {

    private final IngestDocumentUseCase ingest;

    public IngestDocumentTool(IngestDocumentUseCase ingest) {
        this.ingest = ingest;
    }

    @Tool(
        name = "ingest_cargo_document",
        description = """
            Ingest a raw text document into the cargo corpus. The text will be
            chunked, embedded with the configured model, persisted in Postgres,
            and indexed in pgvector. Returns the new document's ID and chunk
            count so subsequent queries can filter to it.
            """
    )
    public Document ingest(
            @ToolParam(description = "Human-readable title") String title,
            @ToolParam(description = "Type: BILL_OF_LADING, COMMERCIAL_INVOICE, "
                    + "LETTER_OF_CREDIT, CHARTER_PARTY, REFERENCE, OTHER, ...") DocumentType type,
            @ToolParam(description = "Optional source URI") String sourceUri,
            @ToolParam(description = "Full text of the document") String text) {
        return ingest.ingest(new IngestCommand(title, type, sourceUri, text));
    }
}
