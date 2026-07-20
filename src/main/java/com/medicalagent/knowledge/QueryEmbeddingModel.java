package com.medicalagent.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

public interface QueryEmbeddingModel extends EmbeddingModel, AutoCloseable {

    Response<Embedding> embedQuery(String query);

    @Override
    default void close() {
    }
}
