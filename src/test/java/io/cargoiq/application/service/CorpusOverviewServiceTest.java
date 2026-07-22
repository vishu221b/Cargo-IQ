package io.cargoiq.application.service;

import io.cargoiq.application.port.in.GetCorpusOverviewUseCase.CorpusOverview;
import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link CorpusOverviewService} with a fake repository.
 *
 * @author Vishal Dogra
 */
class CorpusOverviewServiceTest {

    @Test
    void assemblesOverviewFromRepositoryCounts() {
        var repo = new StubRepository(
                5,
                Map.of(DocumentType.BILL_OF_LADING, 3L, DocumentType.COMMERCIAL_INVOICE, 2L),
                Map.of(Incoterm.CIF, 2L, Incoterm.FOB, 1L));

        CorpusOverview overview = new CorpusOverviewService(repo).overview();

        assertThat(overview.totalDocuments()).isEqualTo(5);
        assertThat(overview.documentsByType())
                .containsEntry(DocumentType.BILL_OF_LADING, 3L)
                .containsEntry(DocumentType.COMMERCIAL_INVOICE, 2L);
        assertThat(overview.documentsByIncoterm())
                .containsEntry(Incoterm.CIF, 2L)
                .containsEntry(Incoterm.FOB, 1L);
    }

    private record StubRepository(
            long total,
            Map<DocumentType, Long> byType,
            Map<Incoterm, Long> byIncoterm) implements DocumentRepository {
        @Override public Document save(Document d) { return d; }
        @Override public Optional<Document> findById(UUID id) { return Optional.empty(); }
        @Override public List<Document> findAll(Optional<DocumentType> t, int limit, int offset) { return List.of(); }
        @Override public void deleteById(UUID id) { }
        @Override public long count() { return total; }
        @Override public Map<DocumentType, Long> countByType() { return byType; }
        @Override public Map<Incoterm, Long> countByIncoterm() { return byIncoterm; }
    }
}
