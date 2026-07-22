package com.medicalagent.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryRecallEvaluatorTest {

    @Test
    void shouldRecallEveryWrittenSessionFactThroughMemorySkills() {
        MemoryRecallEvaluation result = new MemoryRecallEvaluator().evaluate();

        assertEquals(3, result.caseCount());
        assertEquals(3, result.passedCaseCount());
        assertEquals(1d, result.recallAccuracy());
        assertTrue(result.failures().isEmpty());
    }
}
