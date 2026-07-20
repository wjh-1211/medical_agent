package com.medicalagent.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvaluationReportWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldWriteAndReadStructuredReportAndMarkdownSummary() {
        RagEvaluationReport report = new RagEvaluationReport(
                "T013",
                "test-v1",
                "case-v1",
                "corpus-v1",
                "candidate",
                "offline-replay",
                true,
                Instant.now().toString(),
                1,
                new RagEvaluationMetrics(Map.of("retrieval.recall_at_3", 1d)),
                "baseline_created",
                Map.of(),
                Map.of(),
                Map.of("profile", "test"),
                List.of()
        );
        RagEvaluationReportWriter writer = new RagEvaluationReportWriter();
        Path jsonPath = temporaryDirectory.resolve("report.json");
        Path markdownPath = temporaryDirectory.resolve("summary.md");

        writer.writeJson(jsonPath, report);
        writer.writeMarkdownSummary(markdownPath, report);

        assertEquals("candidate", writer.readJson(jsonPath).candidateId());
        assertTrue(markdownPath.toFile().exists());
    }
}
