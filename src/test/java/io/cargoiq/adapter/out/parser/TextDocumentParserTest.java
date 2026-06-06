package io.cargoiq.adapter.out.parser;

import io.cargoiq.application.port.out.DocumentParserPort;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TextDocumentParserTest {

    private final TextDocumentParser parser = new TextDocumentParser();

    @Test
    void extractsBillOfLadingFields() {
        String text = """
                BILL OF LADING
                B/L No: MSCU-7841920
                Vessel: MAERSK SYDNEY
                Port of Loading: Singapore
                Port of Discharge: Brisbane, Australia
                Incoterm: CIF
                Total: USD 124,500.00
                Consignee: Acme Imports Pty Ltd
                """;

        DocumentParserPort.ParseResult result =
                parser.parse(UUID.randomUUID(), DocumentType.BILL_OF_LADING, text);

        var m = result.metadata();
        assertThat(m.blNumber()).contains("MSCU-7841920");
        assertThat(m.vesselName()).contains("MAERSK SYDNEY");
        assertThat(m.portOfLoading()).contains("Singapore");
        assertThat(m.portOfDischarge()).contains("Brisbane");
        assertThat(m.incoterm()).contains(Incoterm.CIF);
        assertThat(m.invoiceValue()).contains(new BigDecimal("124500.00"));
        assertThat(m.currency()).contains("USD");
    }

    @Test
    void producesChunksWithStableSequence() {
        String text = "Lorem ipsum dolor sit amet. ".repeat(200); // ~5400 chars
        var result = parser.parse(UUID.randomUUID(), DocumentType.OTHER, text);

        assertThat(result.chunks()).isNotEmpty();
        for (int i = 0; i < result.chunks().size(); i++) {
            assertThat(result.chunks().get(i).sequence()).isEqualTo(i);
        }
    }

    @Test
    void emptyMetadataWhenNothingMatches() {
        var result = parser.parse(UUID.randomUUID(), DocumentType.OTHER,
                "Just some prose with no shipping fields at all.");

        var m = result.metadata();
        assertThat(m.blNumber()).isEmpty();
        assertThat(m.vesselName()).isEmpty();
        assertThat(m.incoterm()).isEmpty();
    }
}
