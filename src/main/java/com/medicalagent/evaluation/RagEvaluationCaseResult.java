package com.medicalagent.evaluation;

import java.util.List;

public record RagEvaluationCaseResult(
        String caseId,
        String query,
        boolean shouldRetrieve,
        boolean toolCalled,
        List<String> expectedChunkIds,
        List<String> actualChunkIds,
        List<String> actualSources,
        String answer,
        boolean citationPresent,
        boolean citationValid,
        boolean rubricMatched,
        long embeddingMillis,
        long retrievalMillis,
        long toolDecisionMillis,
        long modelMillis,
        long totalMillis,
        List<String> failureReasons
) {
    public RagEvaluationCaseResult {
        expectedChunkIds = expectedChunkIds == null ? List.of() : List.copyOf(expectedChunkIds);
        actualChunkIds = actualChunkIds == null ? List.of() : List.copyOf(actualChunkIds);
        actualSources = actualSources == null ? List.of() : List.copyOf(actualSources);
        failureReasons = failureReasons == null ? List.of() : List.copyOf(failureReasons);
    }
}
