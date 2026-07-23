package com.medicalagent.evaluation;

import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.knowledge.KnowledgeService;
import com.medicalagent.knowledge.KnowledgeServiceFactory;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelGatewayRegistry;
import com.medicalagent.model.PythonTransformersLocalModelGatewayFactory;
import com.medicalagent.model.StubLocalModelGatewayFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RagEvaluationApplication {

    public static void main(String[] args) {
        String profile = System.getProperty("app.profile", "evaluation");
        String mode = System.getProperty("evaluation.mode", "offline");
        AppConfig config = new ConfigLoader().load(profile);
        String retrievalStrategy = System.getProperty("evaluation.retrievalStrategy", config.getKnowledge().getRetrievalStrategy());
        config.getKnowledge().setRetrievalStrategy(retrievalStrategy);
        boolean localMode = "local".equals(mode);
        if (localMode) {
            config.getKnowledge().setDocumentsDirectory("evaluation/corpus");
            config.getKnowledge().setIndexSqlitePath(System.getProperty(
                    "evaluation.localIndexPath",
                    "data/evaluation-local-knowledge-index.db"
            ));
        }
        Path caseSetPath = Path.of(System.getProperty("evaluation.caseSetPath", config.getEvaluation().getCaseSetPath()));
        Path reportPath = Path.of(System.getProperty(
                "evaluation.reportPath",
                localMode ? "evaluation/local-sample-report.json" : config.getEvaluation().getReportPath()
        ));
        Path summaryPath = Path.of(System.getProperty(
                "evaluation.markdownSummaryPath",
                localMode ? "worklog/T013_RAG_LOCAL_SAMPLE.md" : config.getEvaluation().getMarkdownSummaryPath()
        ));
        Path baselinePath = Path.of(System.getProperty("evaluation.baselinePath", config.getEvaluation().getBaselinePath()));
        int topK = Integer.getInteger("evaluation.topK", config.getEvaluation().getTopK());

        KnowledgeService knowledgeService = new KnowledgeServiceFactory()
                .create(config.getKnowledge())
                .orElseThrow(() -> new IllegalStateException("RAG evaluation requires knowledge.enabled=true"));
        try (knowledgeService) {
            knowledgeService.rebuildIndex();
            RagEvaluationCaseSet caseSet = new RagEvaluationCaseLoader().load(caseSetPath);
            if (localMode) {
                int limit = Integer.getInteger("evaluation.localSampleLimit", config.getEvaluation().getLocalSampleLimit());
                caseSet = new RagEvaluationCaseSet(
                        caseSet.caseSetVersion(),
                        caseSet.corpusVersion(),
                        caseSet.cases().stream().limit(limit).toList()
                );
            }
            RagEvaluationReportWriter writer = new RagEvaluationReportWriter();
            Optional<RagEvaluationReport> baseline = !localMode && Files.exists(baselinePath)
                    ? Optional.of(writer.readJson(baselinePath))
                    : Optional.empty();
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("profile", profile);
            metadata.put("embeddingProvider", config.getKnowledge().getEmbeddingProvider());
            metadata.put("embeddingModelPath", config.getKnowledge().getEmbeddingModelPath());
            metadata.put("embeddingDimension", Integer.toString(config.getKnowledge().getEmbeddingDimension()));
            metadata.put("documentsDirectory", config.getKnowledge().getDocumentsDirectory());
            metadata.put("topK", Integer.toString(topK));
            metadata.put("retrievalStrategy", retrievalStrategy);
            metadata.put("evaluationMode", mode);
            metadata.put("modelProvider", config.getModel().getProvider());
            metadata.put("modelName", config.getModel().getName());
            metadata.put("modelTemperature", Double.toString(config.getModel().getTemperature()));
            metadata.put("modelMaxTokens", Integer.toString(config.getModel().getMaxTokens()));
            metadata.put("promptTemplate", config.getPrompt().getDefaultTemplate());
            try (RagEvaluationCandidate candidate = createCandidate(config, knowledgeService, topK, localMode)) {
                RagEvaluationReport report = new RagEvaluationRunner(new RagEvaluationMetricCalculator()).run(
                        caseSet,
                        candidate,
                        topK,
                        config.getEvaluation().getEvaluatorVersion(),
                        metadata,
                        baseline
                );
                writer.writeJson(reportPath, report);
                writer.writeMarkdownSummary(summaryPath, report);
                if (!localMode && baseline.isEmpty()) {
                    writer.writeJson(baselinePath, report);
                }
                System.out.println("RAG evaluation complete: " + reportPath);
                System.out.println("comparison=" + report.comparisonStatus()
                        + " recall@k=" + report.metrics().values().get("retrieval.recall_at_" + topK)
                        + " mrr=" + report.metrics().values().get("retrieval.mrr"));
            }
        }
    }

    private static RagEvaluationCandidate createCandidate(
            AppConfig config,
            KnowledgeService knowledgeService,
            int topK,
            boolean localMode
    ) {
        if (!localMode) {
            return new OfflineReplayRagCandidate(
                    knowledgeService,
                    topK,
                    config.getKnowledge().getRetrievalStrategy() + "-rag-" + config.getKnowledge().getEmbeddingProvider()
            );
        }
        LocalModelGateway modelGateway = new LocalModelGatewayRegistry(List.of(
                new StubLocalModelGatewayFactory(),
                new PythonTransformersLocalModelGatewayFactory()
        )).create(config);
        RecordingLocalModelGateway recordingGateway = new RecordingLocalModelGateway(modelGateway);
        recordingGateway.preload();
        return new LocalAgentRagCandidate(config, knowledgeService, recordingGateway);
    }
}
