package io.cargoiq.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repo for chat messages — internal to the persistence adapter. */
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, UUID> {

    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    long countByConversationId(UUID conversationId);
}
