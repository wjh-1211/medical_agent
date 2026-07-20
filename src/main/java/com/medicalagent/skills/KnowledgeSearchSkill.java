package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.knowledge.KnowledgeChunkMatch;
import com.medicalagent.knowledge.KnowledgeRetriever;

import java.util.List;

public class KnowledgeSearchSkill implements Skill {

    public static final String TOOL_NAME = "knowledge_search";

    private final KnowledgeRetriever knowledgeRetriever;
    private final int defaultTopK;

    public KnowledgeSearchSkill(KnowledgeRetriever knowledgeRetriever, int defaultTopK) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.defaultTopK = defaultTopK;
    }

    @Override
    public String id() {
        return "knowledgeSearchSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query").put("type", "string");
        properties.putObject("topK").put("type", "integer");
        schema.putArray("required").add("query");
        return new ToolSchema(
                TOOL_NAME,
                "Search controlled knowledge documents and return evidence chunks with source identifiers."
                        + " Use it when external factual evidence is needed.",
                schema
        );
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String query = input.path("query").asText().trim();
        int topK = input.has("topK") ? input.path("topK").asInt(defaultTopK) : defaultTopK;
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Knowledge search query must not be blank");
        }
        List<KnowledgeChunkMatch> matches = knowledgeRetriever.search(query, topK);
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("query", query);
        output.put("found", !matches.isEmpty());
        ArrayNode chunks = output.putArray("chunks");
        for (KnowledgeChunkMatch match : matches) {
            ObjectNode chunk = chunks.addObject();
            chunk.put("chunkId", match.chunk().chunkId());
            chunk.put("content", match.chunk().content());
            chunk.put("source", match.chunk().source());
            chunk.put("section", match.chunk().section());
            chunk.put("documentVersion", match.chunk().documentVersion());
            chunk.put("score", match.score());
        }
        return output;
    }
}
