package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.Citation;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: sparse, lexical (keyword / full-text) search over indexed
 * chunk text — the "BM25 half" of hybrid retrieval.
 *
 * <p>Dense vector search is great at meaning but blurs exact tokens; a query
 * for a specific BL number or an unusual vessel name is exactly where a
 * keyword index wins. {@link io.cargoiq.application.service.AnswerQueryService}
 * fuses the two ranked lists with Reciprocal Rank Fusion.
 *
 * <p>The default adapter runs Postgres full-text search against the same
 * {@code vector_store.content} column Spring AI already populates, so no second
 * copy of the text is needed.
 *
 * @author Vishal Dogra
 */
public interface KeywordSearchPort {

    /** Rank chunks by lexical relevance to {@code request.queryText()}. */
    List<Citation> keywordSearch(SearchRequest request);

    record SearchRequest(
            String queryText,
            int topK,
            Optional<DocumentType> filterByType,
            Optional<Incoterm> filterByIncoterm) {}

    /** A no-op implementation — used as a graceful default when hybrid is off. */
    static KeywordSearchPort empty() {
        return request -> List.of();
    }
}
