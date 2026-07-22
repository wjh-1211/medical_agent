package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.config.ModelConfig;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EmergencyDetectionSkill;
import com.medicalagent.skills.GroundingCheckSkill;
import com.medicalagent.skills.RiskAssessmentSkill;
import com.medicalagent.skills.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardrailPipelineTest {

    @Test
    void shouldReplaceCandidateWithEmergencyEscalation() {
        AppConfig config = new ConfigLoader().load("test");
        AgentKernel kernel = createKernel(config, List.of(
                finalAnswer("请在家观察。"),
                directEmergency(true, "emergency"),
                risk("high", true),
                grounding(false)
        ));

        var response = kernel.handle(context(config, "我胸口剧痛而且呼吸困难，嘴唇发青。"));

        assertEquals(config.getGuardrail().getEmergencySafeAnswer(), response.answer());
        assertTrue(response.emergencyFlag());
    }

    @Test
    void shouldLimitHighRiskCandidateAndKeepLowRiskCandidate() {
        AppConfig config = new ConfigLoader().load("test");
        AgentKernel highRiskKernel = createKernel(config, List.of(
                finalAnswer("可以自行使用任何药物。"),
                emergency(false, "none"),
                risk("high", true),
                grounding(false)
        ));
        AgentKernel lowRiskKernel = createKernel(config, List.of(
                finalAnswer("请记录症状变化；若加重或出现危险信号，请及时就医。"),
                emergency(false, "none"),
                risk("low", false),
                grounding(false)
        ));

        var highRiskResponse = highRiskKernel.handle(context(config, "请直接给我详细用药方案。"));
        var lowRiskResponse = lowRiskKernel.handle(context(config, "我只是想了解如何记录轻微咳嗽的变化。"));

        assertEquals(config.getGuardrail().getHighRiskSafeAnswer(), highRiskResponse.answer());
        assertEquals("请记录症状变化；若加重或出现危险信号，请及时就医。", lowRiskResponse.answer());
        assertFalse(lowRiskResponse.emergencyFlag());
    }

    @Test
    void shouldLimitInvalidCitationWhenGroundingRequiresEvidence() {
        AppConfig config = new ConfigLoader().load("test");
        AgentKernel kernel = createKernel(config, List.of(
                finalAnswer("资料说可以自行诊断。 [source: Missing | chunk: a1b2c3d4e5f60718293a4b5c]"),
                emergency(false, "none"),
                risk("low", false),
                grounding(true)
        ));

        var response = kernel.handle(context(config, "请给我受控知识库的来源。"));

        assertEquals(config.getGuardrail().getInsufficientEvidenceSafeAnswer(), response.answer());
    }

    private AgentKernel createKernel(AppConfig config, List<String> responses) {
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new EmergencyDetectionSkill(),
                new RiskAssessmentSkill(),
                new GroundingCheckSkill()
        ));
        return new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()),
                new SequenceGateway(config.getModel(), responses),
                new AgentDecisionParser()
        );
    }

    private com.medicalagent.context.AgentContext context(AppConfig config, String message) {
        return new AgentContextFactory(config).create(new AgentRequest(
                message,
                "guardrail-session",
                "guardrail-user",
                List.of(),
                null,
                null,
                false,
                Map.of("channel", "test")
        ));
    }

    private String finalAnswer(String answer) {
        return "{\"type\":\"final_answer\",\"answer\":\"" + answer.replace("\"", "\\\"") + "\"}";
    }

    private String emergency(boolean emergency, String urgency) {
        return "{\"type\":\"tool_call\",\"toolName\":\"emergency_detection\",\"input\":{"
                + "\"emergency\":" + emergency + ",\"urgency\":\"" + urgency + "\",\"reason\":\"test emergency decision\"}}";
    }

    private String directEmergency(boolean emergency, String urgency) {
        return "{\"emergency\":" + emergency + ",\"urgency\":\"" + urgency
                + "\",\"reason\":\"test direct emergency decision\"}";
    }

    private String risk(String riskLevel, boolean requiresFollowUp) {
        return "{\"type\":\"tool_call\",\"toolName\":\"risk_assessment\",\"input\":{"
                + "\"riskLevel\":\"" + riskLevel + "\",\"requiresFollowUp\":" + requiresFollowUp
                + ",\"reason\":\"test risk decision\"}}";
    }

    private String grounding(boolean requiresEvidence) {
        return "{\"type\":\"tool_call\",\"toolName\":\"grounding_check\",\"input\":{"
                + "\"requiresEvidence\":" + requiresEvidence + ",\"reason\":\"test grounding decision\"}}";
    }

    private static final class SequenceGateway implements LocalModelGateway {

        private final ModelConfig modelConfig;
        private final Queue<String> responses;
        private final List<String> prompts = new ArrayList<>();

        private SequenceGateway(ModelConfig modelConfig, List<String> responses) {
            this.modelConfig = modelConfig;
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public String provider() {
            return "guardrail-sequence-test";
        }

        @Override
        public ModelConfig descriptor() {
            return modelConfig;
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            prompts.add(request.prompt());
            String content = responses.remove();
            return new LocalModelResponse(content, request.modelName(), provider(), 0L, request.prompt().length(), content.length());
        }
    }
}
