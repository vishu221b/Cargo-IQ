package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Query;

import java.util.List;
import java.util.UUID;

/**
 * The RAG result: the grounded answer, whether it was grounded in citations,
 * the retrieval strategy that produced it, and the conversation id (echoed so a
 * client can thread follow-ups).
 */
public record QueryResponse(
        String answer,
        boolean grounded,
        String retrievalStrategy,
        String conversationId,
        List<CitationDto> citations) {

    public static QueryResponse from(Answer answer, Query query) {
        List<CitationDto> dtos = answer.citations().stream()
                .map(CitationDto::from)
                .toList();
        return new QueryResponse(
                answer.text(),
                answer.isGrounded(),
                query.retrieval().label(),
                query.conversationId(),
                dtos);
    }

    public record CitationDto(
            UUID documentId,
            UUID chunkId,
            String documentTitle,
            int chunkSequence,
            String snippet,
            double score) {

        static CitationDto from(io.cargoiq.domain.model.Citation c) {
            return new CitationDto(c.documentId(), c.chunkId(),
                    c.documentTitle(), c.chunkSequence(), c.snippet(), c.score());
        }
    }
}
