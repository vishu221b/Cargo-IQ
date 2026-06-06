package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the outbound persistence port by delegating to Spring Data JPA.
 *
 * <p>Two-way mapping happens here ({@link DocumentEntity#fromDomain},
 * {@link DocumentEntity#toDomain}). Application code only ever sees domain
 * types — Hibernate's proxies and managed-entity rules don't leak across the
 * boundary.
 */
@Repository
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpa;

    public DocumentRepositoryAdapter(DocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Document save(Document document) {
        jpa.save(DocumentEntity.fromDomain(document));
        // The domain object the caller passed in already carries its ID and
        // chunks. Return it as-is; the entity round-trip is purely a write —
        // we don't need to reconstruct a domain object from the saved entity.
        return document;
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return jpa.findById(id).map(DocumentEntity::toDomain);
    }

    @Override
    public List<Document> findAll(Optional<DocumentType> filterByType, int limit) {
        return jpa.findFiltered(filterByType.orElse(null), Limit.of(limit)).stream()
                .map(DocumentEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public Map<DocumentType, Long> countByType() {
        Map<DocumentType, Long> out = new EnumMap<>(DocumentType.class);
        for (Object[] row : jpa.countGroupedByType()) {
            out.put((DocumentType) row[0], (Long) row[1]);
        }
        return out;
    }

    @Override
    public Map<Incoterm, Long> countByIncoterm() {
        Map<Incoterm, Long> out = new EnumMap<>(Incoterm.class);
        for (Object[] row : jpa.countGroupedByIncoterm()) {
            out.put((Incoterm) row[0], (Long) row[1]);
        }
        return out;
    }
}
