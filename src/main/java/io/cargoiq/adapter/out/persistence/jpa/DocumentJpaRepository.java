package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.domain.model.DocumentType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository — framework concern, lives strictly inside the
 * outbound adapter. Nothing outside this package may import it. The
 * application talks to {@link io.cargoiq.application.port.out.DocumentRepository}
 * (the port); the {@link DocumentRepositoryAdapter} bridges the two.
 */
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {

    @Query("""
            select d from DocumentEntity d
             where (:type is null or d.type = :type)
             order by d.ingestedAt desc
            """)
    List<DocumentEntity> findFiltered(@Param("type") DocumentType type, Limit limit);
}
