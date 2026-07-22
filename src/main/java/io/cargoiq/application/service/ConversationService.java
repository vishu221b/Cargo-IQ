package io.cargoiq.application.service;

import io.cargoiq.application.port.in.ManageConversationsUseCase;
import io.cargoiq.application.port.out.ConversationRepository;
import io.cargoiq.domain.exception.ConversationNotFoundException;
import io.cargoiq.domain.model.Conversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Conversation management, always scoped to the owning user.
 *
 * @author Vishal Dogra
 */
@Service
public class ConversationService implements ManageConversationsUseCase {

    /** Title derived from the first message, capped for the list UI. */
    private static final int TITLE_MAX = 80;

    private final ConversationRepository repository;

    public ConversationService(ConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Conversation create(UUID userId) {
        return repository.create(UUID.randomUUID(), userId, null);
    }

    @Override
    public List<Conversation> list(UUID userId) {
        return repository.listByUser(userId);
    }

    @Override
    public ConversationDetail get(UUID conversationId, UUID userId) {
        Conversation c = owned(conversationId, userId);
        return new ConversationDetail(c, repository.messages(conversationId));
    }

    @Override
    @Transactional
    public void delete(UUID conversationId, UUID userId) {
        owned(conversationId, userId); // 404 if missing or not owned
        repository.deleteById(conversationId);
    }

    @Override
    @Transactional
    public void ensureOwned(UUID conversationId, UUID userId, String firstMessageTitle) {
        Optional<Conversation> existing = repository.findById(conversationId);
        if (existing.isEmpty()) {
            repository.create(conversationId, userId, title(firstMessageTitle));
            return;
        }
        if (!existing.get().isOwnedBy(userId)) {
            throw new ConversationNotFoundException(conversationId); // don't leak ownership
        }
        repository.setTitleIfAbsent(conversationId, title(firstMessageTitle));
    }

    private Conversation owned(UUID conversationId, UUID userId) {
        Conversation c = repository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        if (!c.isOwnedBy(userId)) {
            throw new ConversationNotFoundException(conversationId);
        }
        return c;
    }

    private static String title(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) return null;
        String t = firstMessage.strip().replaceAll("\\s+", " ");
        return t.length() <= TITLE_MAX ? t : t.substring(0, TITLE_MAX).trim() + "…";
    }
}
