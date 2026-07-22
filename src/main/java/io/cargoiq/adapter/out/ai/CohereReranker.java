package io.cargoiq.adapter.out.ai;

import io.cargoiq.application.port.out.RerankerPort;
import io.cargoiq.domain.model.Citation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hosted cross-encoder reranker using Cohere Rerank (v2).
 *
 * <p>A true cross-encoder scores each (query, passage) pair jointly, which is
 * more accurate than the lexical {@link MmrReranker} — at the cost of an API
 * call and a key. Enabled with {@code cargoiq.rag.reranker=cohere}; needs
 * {@code COHERE_API_KEY}. This is the "swap MMR for a hosted model" roadmap item:
 * because both sit behind {@link RerankerPort}, nothing in the answering service
 * changes.
 *
 * <p>If the key is missing or the call fails, it degrades gracefully to the
 * input order (truncated) rather than failing the query.
 *
 * @author Vishal Dogra
 */
@Component
@ConditionalOnProperty(name = "cargoiq.rag.reranker", havingValue = "cohere")
public class CohereReranker implements RerankerPort {

    private static final Logger log = LoggerFactory.getLogger(CohereReranker.class);

    private final String apiKey;
    private final String model;
    private final RestClient http;

    public CohereReranker(
            @Value("${COHERE_API_KEY:}") String apiKey,
            @Value("${cargoiq.rag.cohere.model:rerank-v3.5}") String model,
            @Value("${cargoiq.rag.cohere.base-url:https://api.cohere.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("cargoiq.rag.reranker=cohere but COHERE_API_KEY is unset — "
                    + "reranking will pass through until a key is provided.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Citation> rerank(String queryText, List<Citation> candidates, int topN) {
        if (candidates.size() <= 1 || apiKey == null || apiKey.isBlank()) {
            return RerankerPort.identity().rerank(queryText, candidates, topN);
        }
        try {
            List<String> docs = candidates.stream().map(Citation::snippet).toList();
            Map<String, Object> body = Map.of(
                    "model", model,
                    "query", queryText,
                    "documents", docs,
                    "top_n", Math.min(topN, candidates.size()));

            Map<String, Object> resp = http.post()
                    .uri("/v2/rerank")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) (resp == null ? null : resp.get("results"));
            if (results == null || results.isEmpty()) {
                return RerankerPort.identity().rerank(queryText, candidates, topN);
            }

            List<Citation> out = new ArrayList<>(results.size());
            for (Map<String, Object> r : results) {
                int idx = ((Number) r.get("index")).intValue();
                if (idx < 0 || idx >= candidates.size()) continue;
                double score = r.get("relevance_score") instanceof Number n ? n.doubleValue() : 0.0;
                Citation c = candidates.get(idx);
                out.add(new Citation(c.documentId(), c.chunkId(), c.documentTitle(),
                        c.chunkSequence(), c.snippet(), score));
            }
            return out;
        } catch (Exception e) {
            log.warn("Cohere rerank failed ({}); falling back to input order.", e.getMessage());
            return RerankerPort.identity().rerank(queryText, candidates, topN);
        }
    }
}
