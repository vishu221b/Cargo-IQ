package io.cargoiq.adapter.out.ai;

import io.cargoiq.application.port.out.RerankerPort;
import io.cargoiq.domain.model.Citation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maximal Marginal Relevance reranker over lexical (bag-of-words cosine)
 * similarity — dependency-free, no cross-encoder download, no API key.
 *
 * <p>Fused retrieval tends to return clusters of near-duplicate chunks. MMR
 * greedily builds the final list by trading off <i>relevance</i> to the query
 * against <i>redundancy</i> with everything already chosen:
 *
 * <pre>   next = argmax [ λ·rel(q, d) − (1−λ)·max_{s∈selected} sim(d, s) ]</pre>
 *
 * <p>The result is a top-k that packs more distinct facts into the LLM's context
 * window. Each returned citation's {@code score} is set to its query relevance
 * so the UI can show a meaningful, comparable number.
 *
 * @author Vishal Dogra
 */
@Component
public class MmrReranker implements RerankerPort {

    /** Relevance-vs-diversity trade-off. 0.7 leans toward relevance. */
    private static final double LAMBDA = 0.7;

    @Override
    public List<Citation> rerank(String queryText, List<Citation> candidates, int topN) {
        if (candidates.size() <= 1 || topN <= 0) {
            return candidates.size() <= topN ? candidates : List.copyOf(candidates.subList(0, topN));
        }
        Map<String, Double> qv = vector(queryText);
        List<Map<String, Double>> cv = new ArrayList<>(candidates.size());
        double[] rel = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Double> v = vector(candidates.get(i).snippet());
            cv.add(v);
            rel[i] = cosine(qv, v);
        }

        int k = Math.min(topN, candidates.size());
        List<Integer> selected = new ArrayList<>(k);
        Set<Integer> remaining = new HashSet<>();
        for (int i = 0; i < candidates.size(); i++) remaining.add(i);

        while (selected.size() < k && !remaining.isEmpty()) {
            int best = -1;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int i : remaining) {
                double maxSim = 0.0;
                for (int s : selected) {
                    maxSim = Math.max(maxSim, cosine(cv.get(i), cv.get(s)));
                }
                double mmr = LAMBDA * rel[i] - (1 - LAMBDA) * maxSim;
                if (mmr > bestScore) {
                    bestScore = mmr;
                    best = i;
                }
            }
            selected.add(best);
            remaining.remove(best);
        }

        List<Citation> out = new ArrayList<>(selected.size());
        for (int idx : selected) {
            Citation c = candidates.get(idx);
            // Surface the query relevance as the score, clamped to [0,1].
            double s = Math.max(0.0, Math.min(1.0, rel[idx]));
            out.add(new Citation(c.documentId(), c.chunkId(), c.documentTitle(),
                    c.chunkSequence(), c.snippet(), s));
        }
        return out;
    }

    // ---- lexical vector helpers ----

    private static Map<String, Double> vector(String text) {
        Map<String, Double> tf = new HashMap<>();
        if (text == null) return tf;
        for (String tok : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (tok.length() < 2) continue;
            tf.merge(tok, 1.0, Double::sum);
        }
        return tf;
    }

    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0.0;
        for (var e : small.entrySet()) {
            Double o = large.get(e.getKey());
            if (o != null) dot += e.getValue() * o;
        }
        return dot / (norm(a) * norm(b));
    }

    private static double norm(Map<String, Double> v) {
        double sum = 0.0;
        for (double d : v.values()) sum += d * d;
        return Math.sqrt(sum) + 1e-9;
    }
}
