package com.medicalagent.evaluation;

import java.util.List;

public record RagEvaluationCase(
        String caseId,
        String query,
        boolean shouldRetrieve,
        List<String> expectedChunkIds,
        List<String> requiredSources,
        List<String> answerRubric,
        String category,
        String difficulty
) {
    public RagEvaluationCase {
        expectedChunkIds = expectedChunkIds == null ? List.of() : List.copyOf(expectedChunkIds);
        requiredSources = requiredSources == null ? List.of() : List.copyOf(requiredSources);
        answerRubric = answerRubric == null ? List.of() : List.copyOf(answerRubric);
    }
}
