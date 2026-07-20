package com.medicalagent.evaluation;

import java.util.List;

public record RagEvaluationCaseSet(
        String caseSetVersion,
        String corpusVersion,
        List<RagEvaluationCase> cases
) {
    public RagEvaluationCaseSet {
        cases = cases == null ? List.of() : List.copyOf(cases);
    }
}
