package io.cargoiq.application.service;

import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.exception.DocumentNotFoundException;
import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentChunk;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.ShipmentMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test for {@link DeleteDocumentService} using hand-rolled fake
 * ports — no Spring, no Testcontainers. Verifies the two invariants that
 * matter for a delete: it touches <i>both</i> stores, and it does so in the
 * safe order (vectors before the aggregate row).
 *
 * @author Vishal Dogra
 */
class DeleteDocumentServiceTest {

    @Test
    void deletesFromVectorStoreThenRepositoryWhenDocumentExists() {
        var id = UUID.randomUUID();
        var calls = new ArrayList<String>();

        var repo = new RecordingRepository(calls, Optional.of(stubDocument(id)));
        var vectors = new RecordingVectorStore(calls);

        new DeleteDocumentService(repo, vectors).delete(id);

        // Both stores hit, vectors first (safe order: no orphaned ghost vectors).
        assertThat(calls).containsExactly("vector.delete:" + id, "repo.delete:" + id);
    }

    @Test
    void throwsNotFoundAndTouchesNeitherStoreWhenDocumentMissing() {
        var id = UUID.randomUUID();
        var calls = new ArrayList<String>();

        var repo = new RecordingRepository(calls, Optional.empty());
        var vectors = new RecordingVectorStore(calls);

        assertThatThrownBy(() -> new DeleteDocumentService(repo, vectors).delete(id))
                .isInstanceOf(DocumentNotFoundException.class);

        assertThat(calls).isEmpty();
    }

    private static Document stubDocument(UUID id) {
        return new Document(id, "BL-1", DocumentType.BILL_OF_LADING, null,
                ShipmentMetadata.empty(), List.<DocumentChunk>of(), Instant.now());
    }

    // ---- fakes ----

    private record RecordingRepository(List<String> calls, Optional<Document> stored)
            implements DocumentRepository {
        @Override public Document save(Document d) { return d; }
        @Override public Optional<Document> findById(UUID id) { return stored; }
        @Override public List<Document> findAll(Optional<DocumentType> t, int limit) { return List.of(); }
        @Override public void deleteById(UUID id) { calls.add("repo.delete:" + id); }
    }

    private record RecordingVectorStore(List<String> calls) implements VectorStorePort {
        @Override public void index(List<DocumentChunk> c, ChunkMetadata m) { }
        @Override public List<io.cargoiq.domain.model.Citation> similaritySearch(SearchRequest r) { return List.of(); }
        @Override public void deleteByDocumentId(UUID id) { calls.add("vector.delete:" + id); }
    }
}
