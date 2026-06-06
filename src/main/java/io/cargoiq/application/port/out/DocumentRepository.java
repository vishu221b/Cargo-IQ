package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (SPI): persistence for {@link Document} aggregates.
 *
 * <p>Note the asymmetry with Spring Data: <i>we</i> define the contract, the
 * adapter implements it. This is the Dependency Inversion Principle made
 * concrete — the application doesn't depend on JPA; JPA depends on us.
 *
 * <p>The implementation lives in {@code adapter/out/persistence/jpa}.
 */
public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(UUID id);

    List<Document> findAll(Optional<DocumentType> filterByType, int limit);

    void deleteById(UUID id);
}
