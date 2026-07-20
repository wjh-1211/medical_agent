package com.medicalagent.evaluation;

public interface RagEvaluationCandidate extends AutoCloseable {

    String candidateId();

    String mode();

    boolean deterministic();

    RagEvaluationExecution execute(RagEvaluationCase evaluationCase);

    @Override
    default void close() {
    }
}
