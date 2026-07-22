package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.domain.model.Conversation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA model for a chat {@link Conversation}. Messages live in a side table. */
@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(length = 200)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {} // JPA

    public static ConversationEntity create(UUID id, UUID userId, String title, Instant now) {
        var e = new ConversationEntity();
        e.id = id;
        e.userId = userId;
        e.title = title;
        e.createdAt = now;
        e.updatedAt = now;
        return e;
    }

    public Conversation toDomain(long messageCount) {
        return new Conversation(id, userId, title, createdAt, updatedAt, messageCount);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
