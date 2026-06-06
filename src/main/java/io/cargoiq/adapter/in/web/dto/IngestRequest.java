package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngestRequest(
        @NotBlank @Size(max = 256) String title,
        @NotNull DocumentType type,
        @Size(max = 512) String sourceUri,
        @NotBlank @Size(min = 1, max = 5_000_000) String text) {}
