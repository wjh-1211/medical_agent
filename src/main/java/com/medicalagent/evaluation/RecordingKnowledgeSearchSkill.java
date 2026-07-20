package com.medicalagent.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.knowledge.KnowledgeRetriever;
import com.medicalagent.skills.KnowledgeSearchSkill;
import com.medicalagent.skills.ToolSchema;

public class RecordingKnowledgeSearchSkill extends KnowledgeSearchSkill {

    private JsonNode latestResult;
    private long latestLatencyMillis;

    public RecordingKnowledgeSearchSkill(KnowledgeRetriever knowledgeRetriever, int defaultTopK) {
        super(knowledgeRetriever, defaultTopK);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        long startedAt = System.nanoTime();
        latestResult = super.execute(input);
        latestLatencyMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        return latestResult;
    }

    public void reset() {
        latestResult = null;
        latestLatencyMillis = 0L;
    }

    public JsonNode latestResult() {
        return latestResult;
    }

    public long latestLatencyMillis() {
        return latestLatencyMillis;
    }
}
