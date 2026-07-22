package io.cargoiq.application.port.out;

import java.util.List;

/**
 * Outbound port: expand one user question into several retrieval variants
 * (multi-query retrieval).
 *
 * <p>Terse trade-finance questions ("BL for the Brisbane coffee?") under-retrieve
 * because the corpus spells things out ("Bill of Lading", "port of discharge
 * Brisbane"). Rewriting the query into domain-aware variants and unioning the
 * hits lifts recall before fusion/rerank.
 *
 * <p>The default adapter is a dependency-free heuristic expander (synonyms +
 * keyword-only form); it can be swapped for an LLM-backed rewriter without
 * touching the service.
 *
 * @author Vishal Dogra
 */
public interface QueryRewriterPort {

    /**
     * Produce up to {@code maxVariants} query strings, always including the
     * original text first. Never returns an empty list.
     */
    List<String> rewrite(String queryText, int maxVariants);

    /** Identity rewriter — returns just the original query. */
    static QueryRewriterPort identity() {
        return (queryText, maxVariants) -> List.of(queryText);
    }
}
