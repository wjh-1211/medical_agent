package com.medicalagent.evaluation;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvaluationCaseLoaderTest {

    @Test
    void shouldLoadVersionedThirtyCaseSet() {
        RagEvaluationCaseSet caseSet = new RagEvaluationCaseLoader()
                .load(Path.of("evaluation/rag-evaluation-cases.json"));

        assertEquals("rag-eval-v1", caseSet.caseSetVersion());
        assertEquals("eval-v1", caseSet.corpusVersion());
        assertEquals(30, caseSet.cases().size());
        assertTrue(caseSet.cases().stream().anyMatch(evaluationCase -> !evaluationCase.shouldRetrieve()));
        assertTrue(caseSet.cases().stream().anyMatch(
                evaluationCase -> evaluationCase.shouldRetrieve() && evaluationCase.expectedChunkIds().isEmpty()
        ));
    }
}
