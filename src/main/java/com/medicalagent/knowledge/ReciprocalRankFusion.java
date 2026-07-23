package com.medicalagent.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReciprocalRankFusion {

    public List<KnowledgeChunkMatch> fuse(List<KnowledgeChunkMatch> vectorMatches, List<KnowledgeChunkMatch> lexicalMatches, int rrfK) {
        Map<String, FusedMatch> fused = new LinkedHashMap<>();
        add(fused, vectorMatches, rrfK);
        add(fused, lexicalMatches, rrfK);
        return fused.values().stream()
                .map(value -> new KnowledgeChunkMatch(value.chunk(), value.score()))
                .sorted(Comparator.comparingDouble(KnowledgeChunkMatch::score).reversed())
                .toList();
    }

    private void add(Map<String, FusedMatch> fused, List<KnowledgeChunkMatch> matches, int rrfK) {
        for (int index = 0; index < matches.size(); index++) {
            KnowledgeChunkMatch match = matches.get(index);
            int rank = index;
            fused.compute(match.chunk().chunkId(), (id, current) -> {
                double contribution = 1d / (rrfK + rank + 1d);
                return current == null
                        ? new FusedMatch(match.chunk(), contribution)
                        : new FusedMatch(current.chunk(), current.score() + contribution);
            });
        }
    }

    private record FusedMatch(KnowledgeChunk chunk, double score) {
    }
}
