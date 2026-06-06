package io.cargoiq.adapter.out.parser;

import io.cargoiq.application.port.out.DocumentParserPort;
import io.cargoiq.domain.model.DocumentChunk;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import io.cargoiq.domain.model.ShipmentMetadata;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reference implementation of {@link DocumentParserPort}.
 *
 * <p>Two responsibilities:
 * <ol>
 *   <li><b>Chunk</b> the raw text into ~roughly fixed-size windows with
 *       overlap. The naive splitter here is intentional — Spring AI ships a
 *       {@code TokenTextSplitter} you can plug in later. Naive is debuggable,
 *       deterministic, and good enough to demo. A reviewer reading your code
 *       sees what's happening immediately.</li>
 *   <li><b>Extract</b> the small set of structured fields we want as filters
 *       (BL number, vessel, ports, INCOTERM, value). Done with simple regex
 *       — replace with an LLM-based extractor later for higher recall.</li>
 * </ol>
 *
 * <h3>Tuning knobs (constants below)</h3>
 * <pre>
 *   CHUNK_SIZE   — target characters per chunk
 *   CHUNK_OVERLAP — characters carried into the next chunk; reduces "cut-off"
 *                   misses where a fact spans a boundary
 * </pre>
 * For trade-finance docs (semi-structured, lots of short labelled lines)
 * 800/120 is a reasonable starting point. Tune against your eval set.
 */
@Component
public class TextDocumentParser implements DocumentParserPort {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 120;

    // Loose regexes — extraction is best-effort. Misses are fine (Optional.empty()),
    // false-positives are the failure mode to avoid. Keep them strict.
    private static final Pattern BL_NUMBER =
            Pattern.compile("(?i)\\bB/?L(?:\\s*(?:no\\.?|number))?[:\\s]+([A-Z0-9-]{6,20})");
    private static final Pattern VESSEL =
            Pattern.compile("(?i)\\b(?:vessel|m\\.?v\\.?|ship\\s*name)[:\\s]+([A-Z][A-Za-z0-9 .'\\-]{2,40})");
    private static final Pattern PORT_LOADING =
            Pattern.compile("(?i)port\\s+of\\s+loading[:\\s]+([A-Z][A-Za-z .'\\-]+?)(?:\\n|,|;)");
    private static final Pattern PORT_DISCHARGE =
            Pattern.compile("(?i)port\\s+of\\s+discharge[:\\s]+([A-Z][A-Za-z .'\\-]+?)(?:\\n|,|;)");
    private static final Pattern INCOTERM =
            Pattern.compile("\\b(EXW|FCA|CPT|CIP|DAP|DPU|DDP|FAS|FOB|CFR|CIF)\\b");
    private static final Pattern INVOICE_VALUE =
            Pattern.compile("(?i)(?:total|invoice\\s+value|amount)[:\\s]+([A-Z]{3})?\\s*([0-9]{1,3}(?:[,\\s][0-9]{3})*(?:\\.[0-9]+)?)");

    @Override
    public ParseResult parse(UUID documentId, DocumentType type, String rawText) {
        List<DocumentChunk> chunks = chunk(documentId, rawText);
        ShipmentMetadata metadata = extractMetadata(rawText, type);
        return new ParseResult(chunks, metadata);
    }

    // ---- chunking ----

    private List<DocumentChunk> chunk(UUID documentId, String text) {
        String normalized = text.replaceAll("\\r\\n", "\n").trim();
        List<DocumentChunk> out = new ArrayList<>();
        int start = 0;
        int seq = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            // try not to cut mid-word
            if (end < normalized.length()) {
                int boundary = normalized.lastIndexOf('\n', end);
                if (boundary == -1 || boundary <= start + CHUNK_SIZE / 2) {
                    boundary = normalized.lastIndexOf(' ', end);
                }
                if (boundary > start + CHUNK_SIZE / 2) end = boundary;
            }
            String slice = normalized.substring(start, end).trim();
            if (!slice.isBlank()) {
                out.add(DocumentChunk.of(documentId, seq++, slice));
            }
            start = end - CHUNK_OVERLAP;
            if (start <= 0) start = end; // safety
        }
        return out;
    }

    // ---- metadata extraction ----

    private ShipmentMetadata extractMetadata(String text, DocumentType type) {
        var b = ShipmentMetadata.builder();
        firstMatch(BL_NUMBER, text).ifPresent(b::blNumber);
        firstMatch(VESSEL, text).ifPresent(b::vesselName);
        firstMatch(PORT_LOADING, text).map(String::trim).ifPresent(b::portOfLoading);
        firstMatch(PORT_DISCHARGE, text).map(String::trim).ifPresent(b::portOfDischarge);
        firstMatch(INCOTERM, text).flatMap(Incoterm::parse).ifPresent(b::incoterm);
        extractInvoiceValue(text, b);
        return b.build();
    }

    private static java.util.Optional<String> firstMatch(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? java.util.Optional.of(m.group(1)) : java.util.Optional.empty();
    }

    private static void extractInvoiceValue(String text, ShipmentMetadata.Builder b) {
        Matcher m = INVOICE_VALUE.matcher(text);
        if (!m.find()) return;
        String ccy = m.group(1);
        String amount = m.group(2).replaceAll("[,\\s]", "");
        try {
            b.invoiceValue(new BigDecimal(amount));
            if (ccy != null) b.currency(ccy);
        } catch (NumberFormatException ignored) {
            // best-effort
        }
    }
}
