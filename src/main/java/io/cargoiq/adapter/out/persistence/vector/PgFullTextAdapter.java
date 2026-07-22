package io.cargoiq.adapter.out.persistence.vector;

import io.cargoiq.application.port.out.DocumentContentPort;
import io.cargoiq.application.port.out.KeywordSearchPort;
import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lexical (BM25-style) search and whole-document reads over Spring AI's
 * {@code vector_store} table, via plain SQL.
 *
 * <p>Two ports, one table:
 * <ul>
 *   <li>{@link KeywordSearchPort} — Postgres full-text search ({@code to_tsvector}
 *       / {@code plainto_tsquery} ranked by {@code ts_rank}) is the sparse half
 *       of hybrid retrieval. It shares the exact same rows Spring AI writes for
 *       dense search, so there is no second copy of the chunk text.</li>
 *   <li>{@link DocumentContentPort} — reassembles a document's full text from its
 *       chunk rows in sequence order (used by the MCP document resource).</li>
 * </ul>
 *
 * <p>A GIN full-text index is created lazily on first use (best-effort): the
 * {@code vector_store} table is owned by the Spring AI starter and only exists
 * after its schema-init runs, so we can't reference it from a Flyway migration.
 * Search is correct without the index (just a sequential scan) — the index is a
 * pure speed-up once the corpus grows.
 *
 * @author Vishal Dogra
 */
@Component
public class PgFullTextAdapter implements KeywordSearchPort, DocumentContentPort {

    private static final Logger log = LoggerFactory.getLogger(PgFullTextAdapter.class);
    private static final String TS_CONFIG = "english";

    private final JdbcTemplate jdbc;
    private volatile boolean indexEnsured = false;

    public PgFullTextAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Citation> keywordSearch(SearchRequest request) {
        if (request.queryText() == null || request.queryText().isBlank()) {
            return List.of();
        }
        ensureIndex();

        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select id, content, metadata,
                       ts_rank(to_tsvector('%s', content), plainto_tsquery('%s', ?)) as rank
                  from vector_store
                 where to_tsvector('%s', content) @@ plainto_tsquery('%s', ?)
                """.formatted(TS_CONFIG, TS_CONFIG, TS_CONFIG, TS_CONFIG));
        args.add(request.queryText());
        args.add(request.queryText());

        request.filterByType().ifPresent(t -> {
            sql.append(" and metadata ->> 'documentType' = ?");
            args.add(t.name());
        });
        request.filterByIncoterm().ifPresent(i -> {
            sql.append(" and metadata ->> 'incoterm' = ?");
            args.add(i.name());
        });
        sql.append(" order by rank desc limit ?");
        args.add(Math.max(1, request.topK()));

        try {
            return jdbc.query(sql.toString(), (rs, i) -> toCitation(rs.getString("content"),
                    rs.getString("metadata"), rs.getString("id"), rs.getDouble("rank")), args.toArray());
        } catch (DataAccessException e) {
            // vector_store may not exist yet (empty corpus / schema not initialised).
            log.debug("Keyword search skipped ({}). Returning no lexical hits.", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String joinedText(UUID documentId) {
        try {
            List<String> chunks = jdbc.query("""
                    select content from vector_store
                     where metadata ->> 'documentId' = ?
                     order by (metadata ->> 'chunkSequence')::int
                    """, (rs, i) -> rs.getString("content"), documentId.toString());
            return String.join("\n\n", chunks);
        } catch (DataAccessException e) {
            log.debug("joinedText skipped for {} ({})", documentId, e.getMessage());
            return "";
        }
    }

    // ---- helpers ----

    private synchronized void ensureIndex() {
        if (indexEnsured) return;
        try {
            jdbc.execute("create index if not exists idx_vector_store_content_fts "
                    + "on vector_store using gin (to_tsvector('" + TS_CONFIG + "', content))");
            log.info("Full-text index on vector_store.content is present.");
        } catch (DataAccessException e) {
            // Table not there yet, or insufficient privileges — search still works, just slower.
            log.debug("Could not create FTS index yet ({}). Proceeding without it.", e.getMessage());
        }
        indexEnsured = true;
    }

    private Citation toCitation(String content, String metadataJson, String rowId, double rank) {
        String docId = jsonField(metadataJson, "documentId");
        String chunkId = jsonField(metadataJson, "chunkId");
        String title = jsonField(metadataJson, "documentTitle");
        String seqStr = jsonField(metadataJson, "chunkSequence");

        UUID documentId = docId != null ? UUID.fromString(docId) : UUID.fromString(rowId);
        UUID chunkUuid = chunkId != null ? UUID.fromString(chunkId) : UUID.fromString(rowId);
        int seq = parseIntSafe(seqStr);
        String text = content != null ? content : "";
        String snippet = text.length() > 400 ? text.substring(0, 400) + "…" : text;

        return new Citation(documentId, chunkUuid, title != null ? title : "(untitled)", seq, snippet, rank);
    }

    /**
     * Minimal JSON string-field reader — the metadata is a flat object of string
     * values that we wrote ourselves, so a dependency-free extraction keeps this
     * adapter free of a JSON binding. Handles {@code "key":"value"} and numbers.
     */
    private static String jsonField(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\"";
        int k = json.indexOf(needle);
        if (k < 0) return null;
        int colon = json.indexOf(':', k + needle.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;
        if (json.charAt(i) == '"') {
            int end = json.indexOf('"', i + 1);
            return end < 0 ? null : json.substring(i + 1, end);
        }
        int end = i;
        while (end < json.length() && "}],".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(i, end).trim();
    }

    private static int parseIntSafe(String s) {
        try { return s == null ? 0 : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
