package io.cargoiq.adapter.out.persistence.vector;

import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.DocumentChunk;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter that implements {@link VectorStorePort} on top of Spring AI's
 * {@link VectorStore} (auto-configured against pgvector).
 *
 * <p>This is the only place in the codebase that imports Spring AI's
 * {@code VectorStore} / {@code Document} types. Inside the application layer,
 * we deal in {@link io.cargoiq.domain.model.DocumentChunk} and
 * {@link Citation} — domain language. The conversion lives here, in the
 * adapter, where it belongs.
 *
 * <h3>Metadata-driven filtering</h3>
 * We push {@code documentId}, {@code documentTitle}, {@code type}, and
 * {@code incoterm} into each chunk's metadata at index time. At retrieval
 * time we translate the port's {@code SearchRequest} filters into Spring AI's
 * portable {@link Filter} expressions — the same expression compiles to SQL
 * on pgvector, to JSON on Qdrant, etc. Vendor-neutral by design.
 *
 * <h3>Deletion</h3>
 * Spring AI's {@code VectorStore} can delete by filter expression — we use
 * that on {@code documentId} so cascading delete from Postgres → vector store
 * is one cheap call rather than O(chunks).
 */
@Component
public class PgVectorAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(PgVectorAdapter.class);

    /** Metadata key constants — keep these stable across schema versions. */
    static final String META_DOCUMENT_ID = "documentId";
    static final String META_DOCUMENT_TITLE = "documentTitle";
    static final String META_DOCUMENT_TYPE = "documentType";
    static final String META_INCOTERM = "incoterm";
    static final String META_CHUNK_ID = "chunkId";
    static final String META_CHUNK_SEQ = "chunkSequence";

    private final VectorStore vectorStore;

    public PgVectorAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void index(List<DocumentChunk> chunks, ChunkMetadata metadata) {
        if (chunks.isEmpty()) {
            log.warn("index() called with empty chunk list for document {}",
                    metadata.documentId());
            return;
        }

        List<Document> springDocs = chunks.stream()
                .map(c -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put(META_DOCUMENT_ID, metadata.documentId().toString());
                    meta.put(META_DOCUMENT_TITLE, metadata.documentTitle());
                    meta.put(META_DOCUMENT_TYPE, metadata.type().name());
                    metadata.incoterm().ifPresent(i -> meta.put(META_INCOTERM, i.name()));
                    meta.put(META_CHUNK_ID, c.id().toString());
                    meta.put(META_CHUNK_SEQ, c.sequence());
                    // Builder is the stable API across Spring AI 1.1.x — the 3-arg
                    // (id, text, metadata) constructor isn't part of the documented
                    // surface, so the builder is the safer commitment.
                    return Document.builder()
                            .id(c.id().toString())
                            .text(c.text())
                            .metadata(meta)
                            .build();
                })
                .toList();

        vectorStore.add(springDocs);
        log.info("Indexed {} chunks for document {}", chunks.size(), metadata.documentId());
    }

    @Override
    public List<Citation> similaritySearch(SearchRequest request) {
        var b = org.springframework.ai.vectorstore.SearchRequest.builder()
                .query(request.queryText())
                .topK(request.topK());

        Filter.Expression filter = buildFilter(
                request.filterByType(), request.filterByIncoterm());
        if (filter != null) {
            b.filterExpression(filter);
        }

        List<Document> hits = vectorStore.similaritySearch(b.build());
        if (hits == null || hits.isEmpty()) return List.of();

        return hits.stream()
                .map(this::toCitation)
                .toList();
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        Filter.Expression expr = new FilterExpressionBuilder()
                .eq(META_DOCUMENT_ID, documentId.toString())
                .build();
        vectorStore.delete(expr);
    }

    // ---- helpers ----

    /**
     * Build a filter expression from optional facets.
     *
     * <p>Note: {@code FilterExpressionBuilder.eq/and/or/...} return
     * {@code FilterExpressionBuilder.Op}, not {@code Filter.Expression}. We
     * chain Ops together and only call {@code .build()} once, at the end —
     * doing it earlier breaks the call chain because {@code and(...)} requires
     * Ops, not Expressions.
     */
    private Filter.Expression buildFilter(
            java.util.Optional<DocumentType> type,
            java.util.Optional<Incoterm> incoterm) {
        var b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op op = null;
        if (type.isPresent()) {
            op = b.eq(META_DOCUMENT_TYPE, type.get().name());
        }
        if (incoterm.isPresent()) {
            var incOp = b.eq(META_INCOTERM, incoterm.get().name());
            op = (op == null) ? incOp : b.and(op, incOp);
        }
        return op == null ? null : op.build();
    }

    private Citation toCitation(Document hit) {
        Map<String, Object> meta = hit.getMetadata();
        UUID docId = UUID.fromString(meta.get(META_DOCUMENT_ID).toString());
        UUID chunkId = UUID.fromString(meta.getOrDefault(META_CHUNK_ID, hit.getId()).toString());
        String title = String.valueOf(meta.getOrDefault(META_DOCUMENT_TITLE, "(untitled)"));
        int seq = meta.get(META_CHUNK_SEQ) instanceof Number n ? n.intValue() : 0;

        // Spring AI exposes the similarity score on the Document since 1.0.
        double score = hit.getScore() != null ? hit.getScore() : 0.0;

        String text = hit.getText() != null ? hit.getText() : "";
        String snippet = text.length() > 400 ? text.substring(0, 400) + "…" : text;

        return new Citation(docId, chunkId, title, seq, snippet, score);
    }
}
