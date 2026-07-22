package com.medicalagent.skills;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.runtime.ToolRouter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardrailSkillsTest {

    @Test
    void shouldNormalizeEmergencyAndRiskDecisionsThroughToolRouter() {
        ToolRouter router = router();
        ObjectNode emergencyInput = JsonSupport.NODE_FACTORY.objectNode();
        emergencyInput.put("message", "我现在呼吸不舒服");
        emergencyInput.put("emergency", true);
        emergencyInput.put("urgency", "emergency");
        emergencyInput.put("reason", "可能存在需立即升级的风险");

        ObjectNode riskInput = JsonSupport.NODE_FACTORY.objectNode();
        riskInput.put("message", "我想知道详细用药方案");
        riskInput.put("memorySummary", "N/A");
        riskInput.put("riskLevel", "moderate");
        riskInput.put("requiresFollowUp", true);
        riskInput.put("reason", "缺少个体化安全信息");

        var emergency = router.route(EmergencyDetectionSkill.TOOL_NAME, emergencyInput);
        var risk = router.route(RiskAssessmentSkill.TOOL_NAME, riskInput);

        assertTrue(emergency.path("emergency").asBoolean());
        assertEquals("escalate", emergency.path("action").asText());
        assertEquals("limit", risk.path("action").asText());
        assertEquals("moderate", risk.path("riskLevel").asText());
    }

    @Test
    void shouldRejectUnobservedCitationAndAllowObservedCitation() {
        ToolRouter router = router();
        String source = "Controlled Fixture";
        String chunkId = "a1b2c3d4e5f60718293a4b5c";
        ArrayNode observations = JsonSupport.NODE_FACTORY.arrayNode();
        observations.addObject()
                .put("toolName", "knowledge_search")
                .putObject("result")
                .putArray("chunks")
                .addObject()
                .put("source", source)
                .put("chunkId", chunkId);

        ObjectNode validInput = groundingInput(
                "受控资料提示需要评估。 [source: " + source + " | chunk: " + chunkId + "]",
                observations,
                true
        );
        ObjectNode invalidInput = groundingInput(
                "虚构来源。 [source: Missing Source | chunk: f1e2d3c4b5a60718293a4b5c]",
                observations,
                true
        );

        var valid = router.route(GroundingCheckSkill.TOOL_NAME, validInput);
        var invalid = router.route(GroundingCheckSkill.TOOL_NAME, invalidInput);

        assertEquals("allow", valid.path("action").asText());
        assertTrue(valid.path("citationValid").asBoolean());
        assertEquals("limit", invalid.path("action").asText());
        assertFalse(invalid.path("citationValid").asBoolean());
    }

    private ObjectNode groundingInput(String answer, ArrayNode observations, boolean requiresEvidence) {
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("candidateAnswer", answer);
        input.set("observations", observations);
        input.put("requiresEvidence", requiresEvidence);
        input.put("reason", "受控资料事实需要真实来源");
        return input;
    }

    private ToolRouter router() {
        AppConfig config = new ConfigLoader().load("test");
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new EmergencyDetectionSkill(),
                new RiskAssessmentSkill(),
                new GroundingCheckSkill()
        ));
        return new ToolRouter(registry);
    }
}
