package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.application.port.in.GetCorpusOverviewUseCase.CorpusOverview;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Corpus overview response. Enum keys are rendered as their names so the client
 * gets a stable, JSON-friendly object ({@code {"BILL_OF_LADING": 3, ...}}).
 */
public record OverviewResponse(
        long totalDocuments,
        Map<String, Long> documentsByType,
        Map<String, Long> documentsByIncoterm) {

    public static OverviewResponse from(CorpusOverview o) {
        return new OverviewResponse(
                o.totalDocuments(),
                stringKeys(o.documentsByType()),
                stringKeys(o.documentsByIncoterm()));
    }

    private static <K extends Enum<K>> Map<String, Long> stringKeys(Map<K, Long> source) {
        Map<String, Long> out = new LinkedHashMap<>();
        source.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }
}
