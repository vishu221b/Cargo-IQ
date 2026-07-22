package io.cargoiq.adapter.out.reference;

import io.cargoiq.application.port.out.ReferenceDataPort;
import io.cargoiq.domain.model.HsCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory HS-code reference data.
 *
 * <p>Loaded once at startup from {@code classpath:reference/hs-codes.csv}.
 * At this scale that is the right call: the HS schedule is ~5,000 rows at the
 * 6-digit level, easily under a megabyte resident, and avoiding a DB round-trip
 * per lookup keeps the MCP tool snappy.
 *
 * <p>For full 10-digit national tariffs this would be swapped for a JPA-backed
 * adapter — the port doesn't change.
 *
 * <h3>CSV format</h3>
 * <pre>code,description,chapter</pre>
 * Lines starting with {@code #} are treated as comments.
 */
@Component
public class HsCodeReferenceData implements ReferenceDataPort {

    private static final Logger log = LoggerFactory.getLogger(HsCodeReferenceData.class);
    private static final String RESOURCE_PATH = "reference/hs-codes.csv";

    private final Map<String, HsCode> byCode = new HashMap<>();
    private final List<HsCode> all = new ArrayList<>();

    @PostConstruct
    void load() throws IOException {
        var resource = new ClassPathResource(RESOURCE_PATH);
        if (!resource.exists()) {
            log.warn("{} not found on classpath — HS lookups will return empty",
                    RESOURCE_PATH);
            return;
        }
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            int loaded = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                if (header) { header = false; continue; }
                // Descriptions are quoted and often contain commas, so a naive
                // split(",") mis-slices them — parse the quoted CSV properly.
                List<String> parts = splitCsvLine(line);
                if (parts.size() < 3) continue;
                try {
                    var code = new HsCode(parts.get(0).trim(),
                            parts.get(1).trim(),
                            parts.get(2).trim());
                    byCode.put(code.code(), code);
                    all.add(code);
                    loaded++;
                } catch (IllegalArgumentException ex) {
                    log.debug("Skipping malformed HS row: {}", line);
                }
            }
            log.info("Loaded {} HS codes from {}", loaded, RESOURCE_PATH);
        }
    }

    /**
     * Split a single CSV line into fields, honouring double-quoted fields that
     * may contain commas, with {@code ""} as an escaped quote. Enough for the
     * {@code code,description,chapter} schema (and the WCO export shape).
     */
    private static List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }

    @Override
    public Optional<HsCode> findHsCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(byCode.get(code.trim()));
    }

    @Override
    public List<HsCode> searchHsCodes(String descriptionLike, int limit) {
        if (descriptionLike == null || descriptionLike.isBlank()) return List.of();
        String query = descriptionLike.trim();

        // Numeric query → treat as an HS code prefix lookup (e.g. "85" → chapter 85).
        if (query.matches("\\d{2,10}")) {
            return all.stream()
                    .filter(c -> c.code().startsWith(query))
                    .sorted(Comparator.comparing(HsCode::code))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // Free-text: token-aware ranked search (word > prefix > substring), all
        // query terms must contribute. Approximates full-text ranking without a
        // DB round-trip; swap for Postgres FTS behind this port when the schedule
        // grows to national-tariff scale.
        String[] terms = query.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        return all.stream()
                .map(c -> new Scored(c, score(c, terms)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed()
                        .thenComparing(s -> s.code.code()))
                .limit(limit)
                .map(s -> s.code)
                .collect(Collectors.toList());
    }

    /** Sum per-term relevance; returns 0 when any non-blank term matches nothing (AND semantics). */
    private static double score(HsCode c, String[] terms) {
        String desc = c.description().toLowerCase(Locale.ROOT);
        String[] words = desc.split("[^a-z0-9]+");
        double total = 0;
        int meaningfulTerms = 0;
        for (String term : terms) {
            if (term.length() < 2) continue;
            meaningfulTerms++;
            double best = 0;
            for (String w : words) {
                if (w.equals(term)) { best = Math.max(best, 3); }
                else if (w.startsWith(term)) { best = Math.max(best, 2); }
                else if (w.contains(term)) { best = Math.max(best, 1); }
            }
            if (best == 0 && desc.contains(term)) best = 0.5; // spans word boundary
            if (best == 0) return 0; // this term matched nothing → drop the row
            total += best;
        }
        return meaningfulTerms == 0 ? 0 : total;
    }

    private record Scored(HsCode code, double score) {}
}
