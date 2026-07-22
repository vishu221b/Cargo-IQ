package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.domain.model.ChatMessage;
import io.cargoiq.domain.model.ConversationTurn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA model for one turn of a conversation. */
@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConversationTurn.Role role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatMessageEntity() {} // JPA

    public static ChatMessageEntity create(UUID conversationId, ConversationTurn.Role role,
                                           String content, Instant now) {
        var e = new ChatMessageEntity();
        e.id = UUID.randomUUID();
        e.conversationId = conversationId;
        e.role = role;
        e.content = content;
        e.createdAt = now;
        return e;
    }

    public ChatMessage toDomain() {
        return new ChatMessage(id, conversationId, role, content, createdAt);
    }
}
