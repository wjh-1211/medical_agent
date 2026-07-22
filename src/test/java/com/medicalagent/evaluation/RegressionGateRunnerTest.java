package com.medicalagent.evaluation;

import com.medicalagent.common.JsonSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionGateRunnerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldPassWhenEveryReportMetricMeetsItsThreshold() throws Exception {
        RegressionGateReport report = new RegressionGateRunner().run(
                thresholds(),
                write("rag.json", "{\"taskId\":\"T013\",\"metrics\":{\"values\":{\"tool_selection.f1\":1.0,\"answer.rubric_coverage\":0.9}}}"),
                write("guardrail.json", "{\"taskId\":\"T014\",\"metrics\":{\"guardrail.emergency_escalation_rate\":1.0,\"guardrail.case_pass_rate\":1.0}}"),
                write("runtime.json", "{\"taskId\":\"T015\",\"metrics\":{\"tracing.request_correlation_rate\":1.0}}"),
                write("swarm.json", "{\"taskId\":\"T016\",\"metrics\":{\"swarm.valid_plan_execution_rate\":1.0}}")
        );

        assertTrue(report.passed());
        assertTrue(report.failureReasons().isEmpty());
        assertEquals(1d, report.metrics().get("memory.recall_accuracy"));
    }

    @Test
    void shouldFailAndExplainMissingOrBelowThresholdEvidence() throws Exception {
        RegressionGateReport report = new RegressionGateRunner().run(
                thresholds(),
                write("rag.json", "{\"taskId\":\"T013\",\"metrics\":{\"values\":{\"tool_selection.f1\":0.5,\"answer.rubric_coverage\":0.9}}}"),
                temporaryDirectory.resolve("missing-guardrail.json"),
                write("runtime.json", "{\"taskId\":\"T015\",\"metrics\":{\"tracing.request_correlation_rate\":1.0}}"),
                write("swarm.json", "{\"taskId\":\"T016\",\"metrics\":{\"swarm.valid_plan_execution_rate\":1.0}}")
        );

        assertFalse(report.passed());
        assertTrue(report.failureReasons().stream().anyMatch(reason -> reason.contains("guardrail report is missing")));
        assertTrue(report.failureReasons().stream().anyMatch(reason -> reason.startsWith("tool_selection.f1")));
    }

    private RegressionGateThresholds thresholds() {
        return new RegressionGateThresholds("test-v1", Map.of(
                "memory.recall_accuracy", 1d,
                "tool_selection.f1", 1d,
                "planning.success_rate", 1d,
                "safety.emergency_recall", 1d,
                "safety.risk_assessment_accuracy", 1d,
                "response.completeness", 0.85d,
                "tracing.correlation_rate", 1d
        ));
    }

    private Path write(String name, String content) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        JsonSupport.JSON_MAPPER.readTree(content);
        java.nio.file.Files.writeString(path, content);
        return path;
    }
}
