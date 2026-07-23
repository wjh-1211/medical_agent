package com.medicalagent.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory BM25 index rebuilt from the same controlled chunks as the vector index. */
public class Bm25KnowledgeIndex {

    private static final double K1 = 1.2d;
    private static final double B = 0.75d;

    private final LexicalTokenizer tokenizer = new LexicalTokenizer();
    private final List<KnowledgeChunk> chunks;
    private final List<Map<String, Integer>> termFrequencies;
    private final Map<String, Integer> documentFrequencies;
    private final double averageDocumentLength;

    public Bm25KnowledgeIndex(List<KnowledgeChunk> chunks) {
        this.chunks = List.copyOf(chunks == null ? List.of() : chunks);
        this.termFrequencies = new ArrayList<>();
        this.documentFrequencies = new HashMap<>();
        int totalLength = 0;
        for (KnowledgeChunk chunk : this.chunks) {
            Map<String, Integer> frequencies = new HashMap<>();
            tokenizer.tokenize(chunk.content()).forEach(term -> frequencies.merge(term, 1, Integer::sum));
            termFrequencies.add(Map.copyOf(frequencies));
            totalLength += frequencies.values().stream().mapToInt(Integer::intValue).sum();
            frequencies.keySet().forEach(term -> documentFrequencies.merge(term, 1, Integer::sum));
        }
        this.averageDocumentLength = this.chunks.isEmpty() ? 0d : totalLength / (double) this.chunks.size();
    }

    public List<KnowledgeChunkMatch> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0 || chunks.isEmpty()) {
            return List.of();
        }
        List<String> queryTerms = tokenizer.tokenize(query);
        List<KnowledgeChunkMatch> matches = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            Map<String, Integer> frequencies = termFrequencies.get(index);
            int documentLength = frequencies.values().stream().mapToInt(Integer::intValue).sum();
            double score = 0d;
            for (String term : queryTerms) {
                int frequency = frequencies.getOrDefault(term, 0);
                if (frequency == 0) {
                    continue;
                }
                int documentsWithTerm = documentFrequencies.getOrDefault(term, 0);
                double idf = Math.log(1d + (chunks.size() - documentsWithTerm + 0.5d) / (documentsWithTerm + 0.5d));
                double normalization = frequency + K1 * (1d - B + B * documentLength / Math.max(1d, averageDocumentLength));
                score += idf * (frequency * (K1 + 1d)) / normalization;
            }
            if (score > 0d) {
                matches.add(new KnowledgeChunkMatch(chunks.get(index), score));
            }
        }
        return matches.stream().sorted(Comparator.comparingDouble(KnowledgeChunkMatch::score).reversed()).limit(topK).toList();
    }
}
