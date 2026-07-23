package com.medicalagent.knowledge;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Deterministic candidate reranker based on lexical query coverage. */
public class KnowledgeReranker {

    private final LexicalTokenizer tokenizer = new LexicalTokenizer();

    public List<KnowledgeChunkMatch> rerank(String query, List<KnowledgeChunkMatch> candidates, int topK) {
        Set<String> queryTerms = tokenizer.uniqueTokens(query);
        return candidates.stream()
                .map(candidate -> new KnowledgeChunkMatch(candidate.chunk(), score(queryTerms, candidate)))
                .sorted(Comparator.comparingDouble(KnowledgeChunkMatch::score).reversed())
                .limit(topK)
                .toList();
    }

    private double score(Set<String> queryTerms, KnowledgeChunkMatch candidate) {
        Set<String> documentTerms = tokenizer.uniqueTokens(candidate.chunk().content());
        long overlap = queryTerms.stream().filter(documentTerms::contains).count();
        double coverage = queryTerms.isEmpty() ? 0d : overlap / (double) queryTerms.size();
        return candidate.score() + coverage;
    }
}
