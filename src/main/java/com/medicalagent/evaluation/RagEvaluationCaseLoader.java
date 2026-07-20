package com.medicalagent.evaluation;

import com.medicalagent.common.JsonSupport;

import java.io.IOException;
import java.nio.file.Path;

public class RagEvaluationCaseLoader {

    public RagEvaluationCaseSet load(Path path) {
        try {
            RagEvaluationCaseSet caseSet = JsonSupport.JSON_MAPPER.readValue(path.toFile(), RagEvaluationCaseSet.class);
            if (caseSet.cases().isEmpty()) {
                throw new IllegalArgumentException("RAG evaluation case set must not be empty: " + path);
            }
            return caseSet;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load RAG evaluation case set: " + path, exception);
        }
    }
}
