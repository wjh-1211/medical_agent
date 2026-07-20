package com.medicalagent.config;

public class EvaluationConfig {

    private String caseSetPath = "evaluation/rag-evaluation-cases.json";
    private String reportPath = "evaluation-report.json";
    private String markdownSummaryPath = "worklog/T013_RAG_EVALUATION_SUMMARY.md";
    private String baselinePath = "evaluation/baselines/vector-rag-v1.json";
    private int topK = 3;
    private int localSampleLimit = 3;
    private String evaluatorVersion = "t013-v1";

    public String getCaseSetPath() { return caseSetPath; }
    public void setCaseSetPath(String caseSetPath) { this.caseSetPath = caseSetPath; }
    public String getReportPath() { return reportPath; }
    public void setReportPath(String reportPath) { this.reportPath = reportPath; }
    public String getMarkdownSummaryPath() { return markdownSummaryPath; }
    public void setMarkdownSummaryPath(String markdownSummaryPath) { this.markdownSummaryPath = markdownSummaryPath; }
    public String getBaselinePath() { return baselinePath; }
    public void setBaselinePath(String baselinePath) { this.baselinePath = baselinePath; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public int getLocalSampleLimit() { return localSampleLimit; }
    public void setLocalSampleLimit(int localSampleLimit) { this.localSampleLimit = localSampleLimit; }
    public String getEvaluatorVersion() { return evaluatorVersion; }
    public void setEvaluatorVersion(String evaluatorVersion) { this.evaluatorVersion = evaluatorVersion; }
}
