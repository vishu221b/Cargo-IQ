package io.cargoiq.application.port.out;

/**
 * Outbound port: turn an uploaded binary file (PDF, DOCX, HTML, plain text, …)
 * into plain text ready for chunking.
 *
 * <p>Deliberately separate from {@link DocumentParserPort}: the parser's job is
 * text → chunks + structured metadata; this port's job is bytes → text. Keeping
 * them apart means the chunking/extraction logic doesn't care <i>how</i> the
 * text arrived (pasted vs. uploaded), and the binary-format dependency (Apache
 * Tika) stays isolated in one adapter.
 *
 * @author Vishal Dogra
 */
public interface FileTextExtractorPort {

    /**
     * Extract text from an uploaded file.
     *
     * @param bytes       the raw file content
     * @param filename    original filename (used as a Tika hint / for logging)
     * @param contentType MIME type reported by the client (may be null)
     * @return extracted plain text (never null; may be blank for empty files)
     */
    String extract(byte[] bytes, String filename, String contentType);
}
