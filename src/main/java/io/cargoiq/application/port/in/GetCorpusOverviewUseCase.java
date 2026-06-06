package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;

import java.util.Map;

/**
 * Inbound port: a read-only snapshot of the corpus for dashboards and health
 * checks — how many documents are indexed and how they break down by type and
 * INCOTERM. Cheap to compute (aggregate counts), safe for any authenticated
 * user to read.
 *
 * @author Vishal Dogra
 */
public interface GetCorpusOverviewUseCase {

    CorpusOverview overview();

    /**
     * Aggregate view of the corpus. Maps are keyed by the domain enums; the web
     * layer renders the keys as their names for the client.
     */
    record CorpusOverview(
            long totalDocuments,
            Map<DocumentType, Long> documentsByType,
            Map<Incoterm, Long> documentsByIncoterm) {}
}
