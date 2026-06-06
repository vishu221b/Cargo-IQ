package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        DocumentType type,
        String sourceUri,
        Instant ingestedAt,
        int chunkCount,
        MetadataDto metadata) {

    public static DocumentResponse from(Document d) {
        var m = d.metadata();
        return new DocumentResponse(
                d.id(),
                d.title(),
                d.type(),
                d.sourceUri(),
                d.ingestedAt(),
                d.chunkCount(),
                new MetadataDto(
                        m.vesselName().orElse(null),
                        m.blNumber().orElse(null),
                        m.portOfLoading().orElse(null),
                        m.portOfDischarge().orElse(null),
                        m.incoterm().orElse(null),
                        m.invoiceValue().orElse(null),
                        m.currency().orElse(null),
                        m.issueDate().orElse(null),
                        m.shipper().orElse(null),
                        m.consignee().orElse(null)));
    }

    public record MetadataDto(
            String vesselName,
            String blNumber,
            String portOfLoading,
            String portOfDischarge,
            Incoterm incoterm,
            BigDecimal invoiceValue,
            String currency,
            LocalDate issueDate,
            String shipper,
            String consignee) {}
}
