package com.medicalagent.evaluation;

import java.util.List;

public record MemoryRecallEvaluation(int caseCount, int passedCaseCount, double recallAccuracy, List<String> failures) {
    public MemoryRecallEvaluation {
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
