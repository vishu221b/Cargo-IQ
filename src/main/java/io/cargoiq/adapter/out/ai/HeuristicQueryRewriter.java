package io.cargoiq.adapter.out.ai;

import io.cargoiq.application.port.out.QueryRewriterPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dependency-free multi-query expander tuned for trade-finance vocabulary.
 *
 * <p>Rather than call an LLM (and require a key) just to paraphrase a question,
 * this expands the query with domain synonyms the corpus actually uses — a user
 * types "BL", the document says "Bill of Lading" — plus a keyword-only variant
 * with stopwords stripped. The original query is always variant #1.
 *
 * <p>Swap this for an LLM-backed rewriter (behind {@link QueryRewriterPort}) if
 * you want generative paraphrasing; the retrieval service doesn't change.
 *
 * @author Vishal Dogra
 */
@Component
public class HeuristicQueryRewriter implements QueryRewriterPort {

    /** Bidirectional-ish domain synonyms: abbreviation ↔ canonical phrasing. */
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("bl", "bill of lading"),
            Map.entry("b/l", "bill of lading"),
            Map.entry("lc", "letter of credit"),
            Map.entry("l/c", "letter of credit"),
            Map.entry("pod", "port of discharge"),
            Map.entry("pol", "port of loading"),
            Map.entry("eta", "estimated time of arrival"),
            Map.entry("etd", "estimated time of departure"),
            Map.entry("hs", "harmonised system code"),
            Map.entry("coo", "certificate of origin"),
            Map.entry("consignee", "buyer receiver consignee"),
            Map.entry("shipper", "seller exporter shipper"),
            Map.entry("vessel", "vessel ship carrier"),
            Map.entry("incoterm", "incoterms 2020 delivery term"));

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "of", "for", "to", "and", "or", "in", "on", "at", "is", "are",
            "what", "which", "who", "whom", "was", "were", "did", "do", "does", "with", "by",
            "about", "this", "that", "these", "those", "please", "give", "me", "show", "tell");

    @Override
    public List<String> rewrite(String queryText, int maxVariants) {
        if (queryText == null || queryText.isBlank() || maxVariants <= 1) {
            return List.of(queryText);
        }
        Set<String> variants = new LinkedHashSet<>();
        variants.add(queryText.trim());

        String lower = queryText.toLowerCase(Locale.ROOT);
        String[] tokens = lower.split("[^a-z0-9/]+");

        // 1) synonym-expanded variant — append canonical phrasings for any abbreviations present.
        List<String> expansions = new ArrayList<>();
        for (String tok : tokens) {
            String syn = SYNONYMS.get(tok);
            if (syn != null) expansions.add(syn);
        }
        if (!expansions.isEmpty()) {
            variants.add(queryText.trim() + " " + String.join(" ", expansions));
        }

        // 2) keyword-only variant — drop stopwords so retrieval keys on the nouns.
        StringBuilder kw = new StringBuilder();
        for (String tok : tokens) {
            if (tok.isBlank() || STOPWORDS.contains(tok)) continue;
            kw.append(SYNONYMS.getOrDefault(tok, tok)).append(' ');
        }
        String keywords = kw.toString().trim();
        if (!keywords.isBlank() && !keywords.equalsIgnoreCase(queryText.trim())) {
            variants.add(keywords);
        }

        return variants.stream().limit(maxVariants).toList();
    }
}
