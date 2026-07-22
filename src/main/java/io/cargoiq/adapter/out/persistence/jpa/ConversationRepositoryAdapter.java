package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.application.port.out.ConversationRepository;
import io.cargoiq.domain.model.ChatMessage;
import io.cargoiq.domain.model.Conversation;
import io.cargoiq.domain.model.ConversationTurn;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the {@link ConversationRepository} port via Spring Data JPA.
 *
 * @author Vishal Dogra
 */
@Repository
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final ConversationJpaRepository conversations;
    private final ChatMessageJpaRepository messages;

    public ConversationRepositoryAdapter(ConversationJpaRepository conversations,
                                         ChatMessageJpaRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    @Override
    public Conversation create(UUID id, UUID userId, String title) {
        var e = conversations.save(ConversationEntity.create(id, userId, title, Instant.now()));
        return e.toDomain(0);
    }

    @Override
    public Optional<Conversation> findById(UUID id) {
        return conversations.findById(id)
                .map(e -> e.toDomain(messages.countByConversationId(id)));
    }

    @Override
    public List<Conversation> listByUser(UUID userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(e -> e.toDomain(messages.countByConversationId(e.getId())))
                .toList();
    }

    @Override
    public List<ChatMessage> messages(UUID conversationId) {
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(ChatMessageEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void appendMessage(UUID conversationId, ConversationTurn.Role role, String content) {
        var conv = conversations.findById(conversationId).orElse(null);
        if (conv == null) return; // conversation gone — nothing to attach to
        Instant now = Instant.now();
        messages.save(ChatMessageEntity.create(conversationId, role, content, now));
        conv.setUpdatedAt(now);
        conversations.save(conv);
    }

    @Override
    @Transactional
    public void setTitleIfAbsent(UUID conversationId, String title) {
        if (title == null || title.isBlank()) return;
        conversations.findById(conversationId).ifPresent(conv -> {
            if (conv.getTitle() == null || conv.getTitle().isBlank()) {
                conv.setTitle(title);
                conversations.save(conv);
            }
        });
    }

    @Override
    public void deleteById(UUID id) {
        // DB has ON DELETE CASCADE, so this removes the messages too.
        conversations.deleteById(id);
    }
}
