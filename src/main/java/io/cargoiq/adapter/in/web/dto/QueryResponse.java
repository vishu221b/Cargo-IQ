package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.Answer;
import io.cargoiq.domain.model.Citation;

import java.util.List;
import java.util.UUID;

public record QueryResponse(
        String answer,
        boolean grounded,
        List<CitationDto> citations) {

    public static QueryResponse from(Answer answer) {
        List<CitationDto> dtos = answer.citations().stream()
                .map(CitationDto::from)
                .toList();
        return new QueryResponse(answer.text(), answer.isGrounded(), dtos);
    }

    public record CitationDto(
            UUID documentId,
            UUID chunkId,
            String documentTitle,
            int chunkSequence,
            String snippet,
            double score) {

        static CitationDto from(Citation c) {
            return new CitationDto(c.documentId(), c.chunkId(),
                    c.documentTitle(), c.chunkSequence(), c.snippet(), c.score());
        }
    }
}
