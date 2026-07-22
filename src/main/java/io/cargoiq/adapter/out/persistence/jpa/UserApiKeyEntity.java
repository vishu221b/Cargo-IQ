package io.cargoiq.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** JPA model for a user's encrypted API key for one provider. */
@Entity
@Table(name = "user_api_keys")
public class UserApiKeyEntity {

    @EmbeddedId
    private Key id;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "text")
    private String encryptedKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserApiKeyEntity() {} // JPA

    public UserApiKeyEntity(UUID userId, String provider, String encryptedKey) {
        this.id = new Key(userId, provider);
        this.encryptedKey = encryptedKey;
        this.updatedAt = Instant.now();
    }

    public String getEncryptedKey() { return encryptedKey; }
    public String getProvider() { return id.provider; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "user_id", columnDefinition = "uuid")
        private UUID userId;
        @Column(name = "provider", length = 32)
        private String provider;

        protected Key() {}
        public Key(UUID userId, String provider) { this.userId = userId; this.provider = provider; }

        @Override public boolean equals(Object o) {
            return o instanceof Key k && userId.equals(k.userId) && provider.equals(k.provider);
        }
        @Override public int hashCode() { return Objects.hash(userId, provider); }
    }
}
