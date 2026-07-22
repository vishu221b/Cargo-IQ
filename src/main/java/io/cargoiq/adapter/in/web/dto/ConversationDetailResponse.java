package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.application.port.in.ManageConversationsUseCase.ConversationDetail;
import io.cargoiq.domain.model.ChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A conversation plus its full ordered message history. */
public record ConversationDetailResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageDto> messages) {

    public static ConversationDetailResponse from(ConversationDetail detail) {
        var c = detail.conversation();
        return new ConversationDetailResponse(
                c.id(), c.title(), c.createdAt(), c.updatedAt(),
                detail.messages().stream().map(MessageDto::from).toList());
    }

    public record MessageDto(String role, String content, Instant createdAt) {
        static MessageDto from(ChatMessage m) {
            return new MessageDto(m.role().name(), m.content(), m.createdAt());
        }
    }
}
