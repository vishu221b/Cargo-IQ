package io.cargoiq.adapter.out.parser;

import io.cargoiq.application.port.out.FileTextExtractorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link FileTextExtractorPort} backed by Apache Tika (via Spring AI's
 * {@code TikaDocumentReader}).
 *
 * <p>Tika autodetects the format from the bytes (and the filename hint), so a
 * single adapter covers PDF, DOCX, HTML, RTF, plain text and more. This is the
 * only place that knows about binary document formats — the rest of the ingest
 * pipeline sees plain text, exactly as if it had been pasted.
 *
 * @author Vishal Dogra
 */
@Component
public class TikaFileTextExtractor implements FileTextExtractorPort {

    private static final Logger log = LoggerFactory.getLogger(TikaFileTextExtractor.class);

    @Override
    public String extract(byte[] bytes, String filename, String contentType) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        // Expose the filename so Tika can use the extension as a detection hint;
        // it still sniffs the content, so this is best-effort help, not a crutch.
        var resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "upload";
            }
        };
        try {
            List<Document> docs = new TikaDocumentReader(resource).get();
            String text = docs.stream()
                    .map(Document::getText)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining("\n\n"));
            log.info("Extracted {} chars from '{}' ({} bytes, type={})",
                    text.length(), filename, bytes.length, contentType);
            return text;
        } catch (Exception e) {
            log.warn("Tika extraction failed for '{}' ({}): {}", filename, contentType, e.getMessage());
            throw new IllegalArgumentException(
                    "Could not extract text from '" + filename + "'. Is it a supported document format "
                    + "(PDF, DOCX, TXT, HTML)? (" + e.getMessage() + ")");
        }
    }
}
