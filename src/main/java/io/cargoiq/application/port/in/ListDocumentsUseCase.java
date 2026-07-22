package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound port: list / fetch documents already in the corpus. */
public interface ListDocumentsUseCase {

    /** First page (offset 0). */
    default List<Document> list(Optional<DocumentType> filterByType, int limit) {
        return list(filterByType, limit, 0);
    }

    /** A page of documents, newest first, skipping {@code offset} rows. */
    List<Document> list(Optional<DocumentType> filterByType, int limit, int offset);

    Document byId(UUID id);
}
