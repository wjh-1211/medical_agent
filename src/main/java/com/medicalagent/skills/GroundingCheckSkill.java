package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks whether citations in an answer came from this request's knowledge observations. */
public class GroundingCheckSkill implements Skill {

    public static final String TOOL_NAME = "grounding_check";
    private static final Pattern CITATION = Pattern.compile("\\[source:\\s*(.+?)\\s*\\|\\s*chunk:\\s*([0-9a-f]{24})\\s*]");

    @Override
    public String id() {
        return "groundingCheckSkill";
    }

    @Override
    public ToolSchema toolSchema() {
        ObjectNode schema = JsonSupport.NODE_FACTORY.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("candidateAnswer").put("type", "string");
        properties.putObject("observations").put("type", "array");
        properties.putObject("requiresEvidence").put("type", "boolean");
        properties.putObject("reason").put("type", "string");
        schema.putArray("required").add("candidateAnswer").add("observations").add("requiresEvidence").add("reason");
        return new ToolSchema(TOOL_NAME, "Verify that answer citations match actual knowledge-search observations for this request.", schema);
    }

    @Override
    public JsonNode execute(JsonNode input) {
        String answer = input.path("candidateAnswer").asText();
        boolean requiresEvidence = input.path("requiresEvidence").asBoolean();
        String reason = input.path("reason").asText().trim();
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Grounding decision reason must not be blank");
        }
        Set<String> observedCitations = observedCitations(input.path("observations"));
        Matcher matcher = CITATION.matcher(answer);
        boolean citationPresent = false;
        boolean citationValid = true;
        while (matcher.find()) {
            citationPresent = true;
            String key = matcher.group(1).trim() + "|" + matcher.group(2);
            if (!observedCitations.contains(key)) {
                citationValid = false;
            }
        }
        boolean allowed = citationValid && (!requiresEvidence || citationPresent);
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("status", "ok");
        output.put("requiresEvidence", requiresEvidence);
        output.put("citationPresent", citationPresent);
        output.put("citationValid", citationValid);
        output.put("observedCitationCount", observedCitations.size());
        output.put("reason", reason);
        output.put("action", allowed ? "allow" : "limit");
        return output;
    }

    private Set<String> observedCitations(JsonNode observations) {
        Set<String> citations = new HashSet<>();
        for (JsonNode observation : observations) {
            if (!"knowledge_search".equals(observation.path("toolName").asText())) {
                continue;
            }
            for (JsonNode chunk : observation.path("result").path("chunks")) {
                String source = chunk.path("source").asText().trim();
                String chunkId = chunk.path("chunkId").asText().trim();
                if (!source.isBlank() && chunkId.matches("[0-9a-f]{24}")) {
                    citations.add(source + "|" + chunkId);
                }
            }
        }
        return citations;
    }
}
