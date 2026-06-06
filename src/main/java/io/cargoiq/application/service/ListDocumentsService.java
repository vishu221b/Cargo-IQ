package io.cargoiq.application.service;

import io.cargoiq.application.port.in.ListDocumentsUseCase;
import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.domain.exception.DocumentNotFoundException;
import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ListDocumentsService implements ListDocumentsUseCase {

    private final DocumentRepository repository;

    public ListDocumentsService(DocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Document> list(Optional<DocumentType> filterByType, int limit) {
        return repository.findAll(filterByType, limit);
    }

    @Override
    public Document byId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }
}
