package io.cargoiq.adapter.out.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * A dependency-free {@link EmbeddingModel} so the whole RAG pipeline runs with no
 * API key and no model server — the default for local development.
 *
 * <p>It is deliberately <i>not</i> random: it uses signed feature hashing of the
 * text's tokens into a fixed-width, L2-normalised vector. Cosine similarity over
 * such vectors approximates token overlap, so retrieval is lexically meaningful
 * — querying "Brisbane" surfaces chunks that mention Brisbane. That makes the
 * pipeline genuinely demonstrable offline, while a real embedding provider
 * (OpenAI, Ollama, Gemini) gives true semantic recall when configured.
 *
 * <p>The width matches {@code spring.ai.vectorstore.pgvector.dimensions} so the
 * vectors fit the pgvector column.
 *
 * @author Vishal Dogra
 */
public class MockEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public MockEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            embeddings.add(new Embedding(hashEmbed(instructions.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return hashEmbed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        return hashEmbed(text);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private float[] hashEmbed(String text) {
        float[] vec = new float[dimensions];
        if (text != null) {
            for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
                if (token.isEmpty()) continue;
                int h = token.hashCode();
                int idx = Math.floorMod(h, dimensions);
                int sign = ((h >> 31) & 1) == 0 ? 1 : -1; // signed hashing reduces collision bias
                vec[idx] += sign;
            }
        }
        normalize(vec);
        return vec;
    }

    private static void normalize(float[] vec) {
        double sumSq = 0;
        for (float v : vec) sumSq += (double) v * v;
        if (sumSq == 0) {
            vec[0] = 1f; // avoid a zero vector (cosine distance is undefined for it)
            return;
        }
        float norm = (float) Math.sqrt(sumSq);
        for (int i = 0; i < vec.length; i++) vec[i] /= norm;
    }
}
