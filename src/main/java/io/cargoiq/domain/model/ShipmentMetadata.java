package io.cargoiq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Structured fields extracted from a shipping document at parse time.
 *
 * <p>Every field is optional — real-world freight docs are gloriously
 * inconsistent. Treat absent values as "we couldn't extract it", not "it
 * doesn't exist." Downstream queries should filter defensively
 * (e.g. {@code metadata.incoterm().filter(...)}, never {@code .get()}).
 *
 * <p>This is a value object — immutable, equal-by-value. Use the builder for
 * readable construction.
 */
public record ShipmentMetadata(
        Optional<String> vesselName,
        Optional<String> blNumber,
        Optional<String> portOfLoading,
        Optional<String> portOfDischarge,
        Optional<Incoterm> incoterm,
        Optional<BigDecimal> invoiceValue,
        Optional<String> currency,
        Optional<LocalDate> issueDate,
        Optional<String> shipper,
        Optional<String> consignee) {

    public static ShipmentMetadata empty() {
        return new Builder().build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String vesselName;
        private String blNumber;
        private String portOfLoading;
        private String portOfDischarge;
        private Incoterm incoterm;
        private BigDecimal invoiceValue;
        private String currency;
        private LocalDate issueDate;
        private String shipper;
        private String consignee;

        public Builder vesselName(String v)        { this.vesselName = v; return this; }
        public Builder blNumber(String v)          { this.blNumber = v; return this; }
        public Builder portOfLoading(String v)     { this.portOfLoading = v; return this; }
        public Builder portOfDischarge(String v)   { this.portOfDischarge = v; return this; }
        public Builder incoterm(Incoterm v)        { this.incoterm = v; return this; }
        public Builder invoiceValue(BigDecimal v)  { this.invoiceValue = v; return this; }
        public Builder currency(String v)          { this.currency = v; return this; }
        public Builder issueDate(LocalDate v)      { this.issueDate = v; return this; }
        public Builder shipper(String v)           { this.shipper = v; return this; }
        public Builder consignee(String v)         { this.consignee = v; return this; }

        public ShipmentMetadata build() {
            return new ShipmentMetadata(
                    Optional.ofNullable(vesselName),
                    Optional.ofNullable(blNumber),
                    Optional.ofNullable(portOfLoading),
                    Optional.ofNullable(portOfDischarge),
                    Optional.ofNullable(incoterm),
                    Optional.ofNullable(invoiceValue),
                    Optional.ofNullable(currency),
                    Optional.ofNullable(issueDate),
                    Optional.ofNullable(shipper),
                    Optional.ofNullable(consignee));
        }
    }
}
