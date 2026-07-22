package io.cargoiq.domain.model;

import java.util.Objects;

/**
 * One turn in a multi-turn RAG conversation.
 *
 * <p>Conversational memory is the difference between "ask one question" and
 * "have a conversation about a shipment". Prior turns are prepended to the
 * next query so the model can resolve pronouns and follow-ups ("what about its
 * discharge port?") against earlier context.
 *
 * <p>Pure domain — the memory store lives behind {@code ChatMemoryPort}.
 *
 * @author Vishal Dogra
 */
public record ConversationTurn(Role role, String text) {

    public ConversationTurn {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
    }

    /** Who authored a turn. Deliberately tiny — user asks, assistant answers. */
    public enum Role { USER, ASSISTANT }

    public static ConversationTurn user(String text) { return new ConversationTurn(Role.USER, text); }

    public static ConversationTurn assistant(String text) { return new ConversationTurn(Role.ASSISTANT, text); }
}
