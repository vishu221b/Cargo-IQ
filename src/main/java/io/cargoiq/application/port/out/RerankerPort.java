package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.Citation;

import java.util.List;

/**
 * Outbound port: re-order retrieved candidates for final selection.
 *
 * <p>Fused retrieval returns a lot of near-duplicates (adjacent chunks of the
 * same document all match). A reranker re-scores the candidates against the
 * query so the top-k that reaches the LLM is both <i>relevant</i> and
 * <i>diverse</i> — more distinct facts per token of context.
 *
 * <p>The default adapter uses Maximal Marginal Relevance over lexical
 * similarity (no API key, no cross-encoder download); it can be swapped for a
 * hosted rerank model behind the same port.
 *
 * @author Vishal Dogra
 */
public interface RerankerPort {

    /**
     * Return at most {@code topN} citations, re-ordered best-first. The returned
     * citations may carry an updated {@code score} reflecting rerank relevance.
     */
    List<Citation> rerank(String queryText, List<Citation> candidates, int topN);

    /** Identity reranker — keep input order, just truncate to {@code topN}. */
    static RerankerPort identity() {
        return (queryText, candidates, topN) ->
                candidates.size() <= topN ? candidates : List.copyOf(candidates.subList(0, topN));
    }
}
