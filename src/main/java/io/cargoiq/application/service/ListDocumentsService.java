package io.cargoiq.application.service;

import io.cargoiq.application.port.in.ListDocumentsUseCase;
import io.cargoiq.application.port.out.DocumentContentPort;
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
    private final DocumentContentPort documentContent;

    public ListDocumentsService(DocumentRepository repository, DocumentContentPort documentContent) {
        this.repository = repository;
        this.documentContent = documentContent;
    }

    @Override
    public List<Document> list(Optional<DocumentType> filterByType, int limit, int offset) {
        return repository.findAll(filterByType, limit, Math.max(0, offset));
    }

    @Override
    public Document byId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Override
    public String content(UUID id) {
        // Validate existence first (404 vs. empty text for a real-but-unindexed doc).
        byId(id);
        return documentContent.joinedText(id);
    }
}
