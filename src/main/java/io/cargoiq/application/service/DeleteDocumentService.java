package io.cargoiq.application.service;

import io.cargoiq.application.port.in.DeleteDocumentUseCase;
import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.exception.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: delete a document from both backing stores.
 *
 * <pre>
 *   id
 *    │
 *    ▼  DocumentRepository.findById      (exists? else 404)
 *    │
 *    ▼  VectorStorePort.deleteByDocumentId   (drop chunks + embeddings)
 *    │
 *    ▼  DocumentRepository.deleteById         (drop aggregate row)
 * </pre>
 *
 * <p>Order matters: we remove the vectors first, then the aggregate. If the
 * vector delete fails the JPA row survives and the operation is safely
 * retryable; if we deleted the row first and the vector delete then failed we
 * would be left with vectors no aggregate points at — the worse failure mode
 * because retrieval would keep surfacing a "ghost" document. The vector-store
 * write is not part of the JPA transaction, so this ordering is the
 * compensation strategy, not the {@link Transactional} boundary.
 *
 * @author Vishal Dogra
 */
@Service
public class DeleteDocumentService implements DeleteDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteDocumentService.class);

    private final DocumentRepository repository;
    private final VectorStorePort vectorStore;

    public DeleteDocumentService(DocumentRepository repository, VectorStorePort vectorStore) {
        this.repository = repository;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (repository.findById(id).isEmpty()) {
            throw new DocumentNotFoundException(id);
        }
        log.info("Deleting document {} from vector store + aggregate", id);
        vectorStore.deleteByDocumentId(id);
        repository.deleteById(id);
        log.info("Deleted document {}", id);
    }
}
