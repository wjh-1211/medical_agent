package com.medicalagent.evaluation;

import com.medicalagent.knowledge.KnowledgeChunk;
import com.medicalagent.knowledge.KnowledgeChunkMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvaluationRunnerTest {

    @Test
    void shouldMarkFirstRunAsBaselineThenCalculateCandidateDeltas() {
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "case-1", "question", true, List.of("chunk-1"), List.of("source"), List.of("fact"), "hit", "easy"
        );
        RagEvaluationCaseSet caseSet = new RagEvaluationCaseSet("v1", "corpus-v1", List.of(evaluationCase));
        RagEvaluationRunner runner = new RagEvaluationRunner(new RagEvaluationMetricCalculator());

        RagEvaluationReport baseline = runner.run(
                caseSet,
                new FixedCandidate(true),
                3,
                "test",
                Map.of(),
                Optional.empty()
        );
        RagEvaluationReport candidate = runner.run(
                caseSet,
                new FixedCandidate(false),
                3,
                "test",
                Map.of(),
                Optional.of(baseline)
        );

        assertEquals("baseline_created", baseline.comparisonStatus());
        assertEquals("candidate_compared_to_baseline", candidate.comparisonStatus());
        assertEquals(-1d, candidate.metricDeltas().get("retrieval.recall_at_1"));
        assertTrue(candidate.cases().get(0).failureReasons().contains("expected_chunk_not_retrieved"));
    }

    private static final class FixedCandidate implements RagEvaluationCandidate {

        private final boolean hit;

        private FixedCandidate(boolean hit) {
            this.hit = hit;
        }

        @Override
        public String candidateId() {
            return hit ? "hit" : "miss";
        }

        @Override
        public String mode() {
            return "test";
        }

        @Override
        public boolean deterministic() {
            return true;
        }

        @Override
        public RagEvaluationExecution execute(RagEvaluationCase evaluationCase) {
            List<KnowledgeChunkMatch> matches = hit
                    ? List.of(new KnowledgeChunkMatch(
                            new KnowledgeChunk("chunk-1", "fact", "source", "section", "v1", "hash"),
                            0.9d
                    ))
                    : List.of();
            String answer = hit ? "fact [source: source | chunk: chunk-1]" : "资料不足";
            return new RagEvaluationExecution(hit, matches, answer, 0L, 0L, 0L, 0L, 1L);
        }
    }
}
