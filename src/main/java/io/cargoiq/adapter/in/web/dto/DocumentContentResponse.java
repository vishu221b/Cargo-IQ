package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.DocumentType;

import java.util.UUID;

/** A document's full extracted text, for the in-app viewer. */
public record DocumentContentResponse(
        UUID id,
        String title,
        DocumentType type,
        String content) {}
