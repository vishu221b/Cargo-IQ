package io.cargoiq.domain.model;

/**
 * A coarse-grained authorization role. Kept deliberately small for v1 RBAC:
 *
 * <ul>
 *   <li>{@link #USER} — can read the corpus and run RAG queries.</li>
 *   <li>{@link #ADMIN} — can additionally ingest and delete documents.</li>
 * </ul>
 *
 * <p>The {@link #authority()} string follows Spring Security's {@code ROLE_}
 * convention so {@code hasRole('ADMIN')} resolves against {@code ROLE_ADMIN}.
 * That coupling is intentional and lives only at the edges (the JWT claim and
 * the security filter chain); the domain otherwise treats this as a plain enum.
 */
public enum Role {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
