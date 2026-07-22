package com.medicalagent.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregates deterministic task reports into one release decision. */
public class RegressionGateRunner {

    public RegressionGateReport run(
            RegressionGateThresholds thresholds,
            Path ragReportPath,
            Path guardrailReportPath,
            Path runtimeReportPath,
            Path swarmReportPath
    ) {
        List<String> failures = new ArrayList<>();
        Map<String, String> inputs = new LinkedHashMap<>();
        JsonNode rag = readReport("rag", ragReportPath, inputs, failures);
        JsonNode guardrail = readReport("guardrail", guardrailReportPath, inputs, failures);
        JsonNode runtime = readReport("runtime", runtimeReportPath, inputs, failures);
        JsonNode swarm = readReport("swarm", swarmReportPath, inputs, failures);

        MemoryRecallEvaluation memory = new MemoryRecallEvaluator().evaluate();
        if (!memory.failures().isEmpty()) {
            failures.add("memory recall evaluator failed cases: " + String.join(",", memory.failures()));
        }
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("memory.recall_accuracy", memory.recallAccuracy());
        metrics.put("tool_selection.f1", metric(rag, "metrics", "values", "tool_selection.f1", failures));
        metrics.put("planning.success_rate", metric(swarm, "metrics", "swarm.valid_plan_execution_rate", failures));
        metrics.put("safety.emergency_recall", metric(guardrail, "metrics", "guardrail.emergency_escalation_rate", failures));
        metrics.put("safety.risk_assessment_accuracy", metric(guardrail, "metrics", "guardrail.case_pass_rate", failures));
        metrics.put("response.completeness", metric(rag, "metrics", "values", "answer.rubric_coverage", failures));
        metrics.put("tracing.correlation_rate", metric(runtime, "metrics", "tracing.request_correlation_rate", failures));

        List<RegressionGateCheck> checks = new ArrayList<>();
        for (Map.Entry<String, Double> threshold : thresholds.minimumMetrics().entrySet()) {
            double actual = metrics.getOrDefault(threshold.getKey(), 0d);
            boolean passed = actual >= threshold.getValue();
            String failure = passed ? "" : "actual %.4f is below minimum %.4f".formatted(actual, threshold.getValue());
            checks.add(new RegressionGateCheck(threshold.getKey(), actual, threshold.getValue(), passed, failure));
            if (!passed) {
                failures.add(threshold.getKey() + ": " + failure);
            }
        }
        return new RegressionGateReport(
                "T017",
                thresholds.version(),
                Instant.now().toString(),
                failures.isEmpty(),
                inputs,
                metrics,
                checks,
                failures
        );
    }

    private JsonNode readReport(String name, Path path, Map<String, String> inputs, List<String> failures) {
        if (!Files.exists(path)) {
            failures.add(name + " report is missing: " + path);
            inputs.put(name, "missing:" + path);
            return JsonSupport.NODE_FACTORY.objectNode();
        }
        try {
            JsonNode root = JsonSupport.JSON_MAPPER.readTree(path.toFile());
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("root must be a JSON object");
            }
            inputs.put(name, root.path("taskId").asText("unknown") + "@" + path);
            return root;
        } catch (IOException | IllegalArgumentException exception) {
            failures.add(name + " report is invalid: " + path + " (" + exception.getMessage() + ")");
            inputs.put(name, "invalid:" + path);
            return JsonSupport.NODE_FACTORY.objectNode();
        }
    }

    private double metric(JsonNode root, String first, String second, String metric, List<String> failures) {
        return metric(root.path(first).path(second).path(metric), metric, failures);
    }

    private double metric(JsonNode root, String first, String metric, List<String> failures) {
        return metric(root.path(first).path(metric), metric, failures);
    }

    private double metric(JsonNode value, String name, List<String> failures) {
        if (!value.isNumber()) {
            failures.add("required metric is missing: " + name);
            return 0d;
        }
        return value.asDouble();
    }
}
