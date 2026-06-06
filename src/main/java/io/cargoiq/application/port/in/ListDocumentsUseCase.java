package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound port: list / fetch documents already in the corpus. */
public interface ListDocumentsUseCase {

    List<Document> list(Optional<DocumentType> filterByType, int limit);

    Document byId(UUID id);
}
