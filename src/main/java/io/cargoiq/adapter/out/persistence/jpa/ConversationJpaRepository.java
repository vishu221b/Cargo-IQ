package io.cargoiq.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repo for conversations — internal to the persistence adapter. */
public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

    List<ConversationEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
