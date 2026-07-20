package com.medicalagent.evaluation;

import com.medicalagent.knowledge.KnowledgeChunkMatch;

import java.util.List;

public record RagEvaluationExecution(
        boolean toolCalled,
        List<KnowledgeChunkMatch> matches,
        String answer,
        long embeddingMillis,
        long retrievalMillis,
        long toolDecisionMillis,
        long modelMillis,
        long totalMillis
) {
    public RagEvaluationExecution {
        matches = matches == null ? List.of() : List.copyOf(matches);
        answer = answer == null ? "" : answer;
    }
}
