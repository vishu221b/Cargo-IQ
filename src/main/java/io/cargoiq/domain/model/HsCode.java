package io.cargoiq.domain.model;

import java.util.Objects;

/**
 * A Harmonised System code — the 6+ digit taxonomy customs authorities use to
 * classify goods worldwide. Countries extend the 6-digit root (HS) with 2–4
 * additional digits (HTS in the US, AHECC in Australia, etc.).
 *
 * <p>We accept any length from 4 to 10 to support both Chapter-level browsing
 * and full national tariff lookup.
 */
public record HsCode(String code, String description, String chapter) {

    public HsCode {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(description, "description");
        if (code.length() < 4 || code.length() > 10 || !code.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "HS code must be 4-10 digits, got: " + code);
        }
    }

    /** Chapter (first two digits) — useful for grouping. */
    public String chapterCode() { return code.substring(0, 2); }
}
