package com.medicalagent.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;

import java.util.List;

/** Deterministic test-only EmbeddingModel that keeps RAG tests offline. */
public class HashEmbeddingModel implements QueryEmbeddingModel {

    private final int dimension;

    public HashEmbeddingModel(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        return Response.from(segments.stream().map(segment -> embedText(segment.text())).toList());
    }

    @Override
    public Response<Embedding> embedQuery(String query) {
        return Response.from(embedText(query));
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private Embedding embedText(String text) {
        float[] vector = new float[dimension];
        int[] codePoints = text == null ? new int[0] : text.codePoints().toArray();
        for (int index = 0; index < codePoints.length; index++) {
            int current = codePoints[index];
            vector[Math.floorMod(current, dimension)] += 1f;
            if (index + 1 < codePoints.length) {
                int bigram = 31 * current + codePoints[index + 1];
                vector[Math.floorMod(bigram, dimension)] += 1f;
            }
        }
        normalize(vector);
        return Embedding.from(vector);
    }

    private void normalize(float[] vector) {
        double sum = 0d;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0d) {
            return;
        }
        float divisor = (float) Math.sqrt(sum);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= divisor;
        }
    }
}
