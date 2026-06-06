package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(
        @NotBlank @Size(max = 2000) String query,
        @Min(1) @Max(50) Integer topK,
        DocumentType filterByType,
        Incoterm filterByIncoterm) {

    public int topKOrDefault() { return topK != null ? topK : 6; }
}
