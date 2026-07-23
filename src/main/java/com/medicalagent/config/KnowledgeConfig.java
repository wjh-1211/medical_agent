package com.medicalagent.config;

public class KnowledgeConfig {

    private boolean enabled = true;
    private String documentsDirectory = "knowledge";
    private String indexSqlitePath = "data/knowledge-index.db";
    private String embeddingProvider = "python_transformers";
    private String embeddingModelPath = "/mnt/Data/multi-agent/Qwen/Qwen3-Embedding-4B";
    private String embeddingPythonExecutable = "python3";
    private String embeddingLauncherScript = "scripts/local_embedding_inference.py";
    private int embeddingDimension = 1024;
    private int chunkMaxCharacters = 700;
    private int chunkOverlapCharacters = 100;
    private int defaultTopK = 3;
    private double minScore = 0.35d;
    private String retrievalStrategy = "vector";
    private int hybridCandidateMultiplier = 3;
    private int rrfK = 60;
    private boolean rerankEnabled = true;
    private boolean hybridRequireLexicalMatch = true;
    private String queryInstruction = "Given a medical knowledge question, retrieve relevant passages that answer the question";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDocumentsDirectory() { return documentsDirectory; }
    public void setDocumentsDirectory(String documentsDirectory) { this.documentsDirectory = documentsDirectory; }
    public String getIndexSqlitePath() { return indexSqlitePath; }
    public void setIndexSqlitePath(String indexSqlitePath) { this.indexSqlitePath = indexSqlitePath; }
    public String getEmbeddingProvider() { return embeddingProvider; }
    public void setEmbeddingProvider(String embeddingProvider) { this.embeddingProvider = embeddingProvider; }
    public String getEmbeddingModelPath() { return embeddingModelPath; }
    public void setEmbeddingModelPath(String embeddingModelPath) { this.embeddingModelPath = embeddingModelPath; }
    public String getEmbeddingPythonExecutable() { return embeddingPythonExecutable; }
    public void setEmbeddingPythonExecutable(String embeddingPythonExecutable) { this.embeddingPythonExecutable = embeddingPythonExecutable; }
    public String getEmbeddingLauncherScript() { return embeddingLauncherScript; }
    public void setEmbeddingLauncherScript(String embeddingLauncherScript) { this.embeddingLauncherScript = embeddingLauncherScript; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public int getChunkMaxCharacters() { return chunkMaxCharacters; }
    public void setChunkMaxCharacters(int chunkMaxCharacters) { this.chunkMaxCharacters = chunkMaxCharacters; }
    public int getChunkOverlapCharacters() { return chunkOverlapCharacters; }
    public void setChunkOverlapCharacters(int chunkOverlapCharacters) { this.chunkOverlapCharacters = chunkOverlapCharacters; }
    public int getDefaultTopK() { return defaultTopK; }
    public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }
    public double getMinScore() { return minScore; }
    public void setMinScore(double minScore) { this.minScore = minScore; }
    public String getRetrievalStrategy() { return retrievalStrategy; }
    public void setRetrievalStrategy(String retrievalStrategy) { this.retrievalStrategy = retrievalStrategy; }
    public int getHybridCandidateMultiplier() { return hybridCandidateMultiplier; }
    public void setHybridCandidateMultiplier(int hybridCandidateMultiplier) { this.hybridCandidateMultiplier = hybridCandidateMultiplier; }
    public int getRrfK() { return rrfK; }
    public void setRrfK(int rrfK) { this.rrfK = rrfK; }
    public boolean isRerankEnabled() { return rerankEnabled; }
    public void setRerankEnabled(boolean rerankEnabled) { this.rerankEnabled = rerankEnabled; }
    public boolean isHybridRequireLexicalMatch() { return hybridRequireLexicalMatch; }
    public void setHybridRequireLexicalMatch(boolean hybridRequireLexicalMatch) { this.hybridRequireLexicalMatch = hybridRequireLexicalMatch; }
    public String getQueryInstruction() { return queryInstruction; }
    public void setQueryInstruction(String queryInstruction) { this.queryInstruction = queryInstruction; }
}
