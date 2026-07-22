package io.cargoiq.domain.model;

/**
 * Per-query switches for the retrieval pipeline.
 *
 * <p>The RAG pipeline has three optional quality stages layered on top of the
 * baseline vector search. Exposing them per request lets a caller trade latency
 * for recall/precision, and lets the UI show what actually ran:
 *
 * <ul>
 *   <li><b>hybrid</b> — fuse dense (vector) retrieval with sparse (BM25/full-text
 *       keyword) retrieval using Reciprocal Rank Fusion. Catches exact-token
 *       matches (a BL number, a vessel name) that embeddings smear over.</li>
 *   <li><b>multiQuery</b> — expand the question into several trade-domain
 *       variants (BL ↔ bill of lading, POD ↔ port of discharge, …), retrieve for
 *       each, and union the hits before fusion. Boosts recall on terse queries.</li>
 *   <li><b>rerank</b> — re-order the fused candidates with a Maximal Marginal
 *       Relevance pass so the top-k is both relevant <i>and</i> non-redundant.</li>
 * </ul>
 *
 * <p>All default to on — every stage is dependency-free (no extra API key), so
 * "best quality" is the safe default.
 *
 * @author Vishal Dogra
 */
public record RetrievalOptions(boolean hybrid, boolean multiQuery, boolean rerank) {

    private static final RetrievalOptions DEFAULTS = new RetrievalOptions(true, true, true);

    /** All stages enabled — the recommended default. */
    public static RetrievalOptions defaults() { return DEFAULTS; }

    /** Baseline dense-only vector search, no fusion/expansion/rerank. */
    public static RetrievalOptions plain() { return new RetrievalOptions(false, false, false); }

    /** Human-readable label of the active strategy, for logs and the UI. */
    public String label() {
        if (!hybrid && !multiQuery && !rerank) return "vector";
        var sb = new StringBuilder(hybrid ? "hybrid" : "vector");
        if (multiQuery) sb.append(" + multi-query");
        if (rerank) sb.append(" + rerank");
        return sb.toString();
    }
}
