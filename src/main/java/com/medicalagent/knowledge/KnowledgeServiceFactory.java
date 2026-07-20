package com.medicalagent.knowledge;

import com.medicalagent.config.KnowledgeConfig;

import java.nio.file.Path;
import java.util.Optional;

public class KnowledgeServiceFactory {

    public Optional<KnowledgeService> create(KnowledgeConfig config) {
        if (!config.isEnabled()) {
            return Optional.empty();
        }
        QueryEmbeddingModel embeddingModel = switch (config.getEmbeddingProvider()) {
            case "python_transformers" -> new PythonTransformersEmbeddingModel(config);
            case "hash" -> new HashEmbeddingModel(config.getEmbeddingDimension());
            default -> throw new IllegalArgumentException("Unsupported knowledge.embeddingProvider: " + config.getEmbeddingProvider());
        };
        return Optional.of(new KnowledgeService(
                config,
                embeddingModel,
                new SqliteKnowledgeVectorStore(Path.of(config.getIndexSqlitePath())),
                new KnowledgeDocumentLoader()
        ));
    }
}
