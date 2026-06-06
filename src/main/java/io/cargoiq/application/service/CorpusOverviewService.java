package io.cargoiq.application.service;

import io.cargoiq.application.port.in.GetCorpusOverviewUseCase;
import io.cargoiq.application.port.out.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: assemble the corpus overview from the repository's aggregate counts.
 *
 * <p>Thin by design — the counting is pushed down to the persistence adapter
 * (a {@code group by} in SQL) rather than pulled into memory here. The read is
 * marked {@link Transactional} read-only so the three count queries share one
 * connection and a consistent snapshot.
 *
 * @author Vishal Dogra
 */
@Service
public class CorpusOverviewService implements GetCorpusOverviewUseCase {

    private final DocumentRepository repository;

    public CorpusOverviewService(DocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CorpusOverview overview() {
        return new CorpusOverview(
                repository.count(),
                repository.countByType(),
                repository.countByIncoterm());
    }
}
