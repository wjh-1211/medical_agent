package com.medicalagent.agent;

import com.medicalagent.api.AgentMessage;
import com.medicalagent.api.AgentRequest;
import com.medicalagent.api.AgentResponse;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.SessionMemoryStore;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.config.ModelConfig;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentKernelMedicalFollowUpTest {

    @Test
    void shouldAskForMissingSafetyContextBeforeDetailedMedicationAdvice() {
        AppConfig config = new ConfigLoader().load("test");
        SessionMemoryStore memoryStore = new InMemorySessionMemoryStore(
                Duration.ofMinutes(config.getMemory().getSessionTtlMinutes())
        );
        SkillRegistry registry = new SkillRegistry(config, List.of(
                new EchoSkill(),
                new UppercaseSkill(),
                new MemoryReadSkill(memoryStore),
                new MemoryWriteSkill(memoryStore)
        ));
        SequenceLocalModelGateway gateway = new SequenceLocalModelGateway(config.getModel(), List.of(
                "{\"type\":\"final_answer\",\"answer\":\"感冒时可以先休息和补水，并观察症状变化。\"}",
                "{\"type\":\"final_answer\",\"answer\":\"为了更安全地提供详细用药信息，请先补充：年龄段；是否发热及大致体温；症状持续时间和主要症状；是否有基础病、药物过敏史或正在服用其他药物。补齐后我再说明一般注意事项；如症状严重或不确定，请咨询医生或药师。\"}"
        ));
        AgentKernel kernel = new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(config.getPrompt()),
                gateway,
                new AgentDecisionParser()
        );
        AgentContextFactory contextFactory = new AgentContextFactory(config);

        AgentResponse firstResponse = kernel.handle(contextFactory.create(new AgentRequest(
                "我现在感冒了，你能给我一些建议嘛",
                "medical-follow-up",
                "user-1",
                List.of(),
                null,
                null,
                false,
                Map.of("channel", "test")
        )));

        AgentResponse secondResponse = kernel.handle(contextFactory.create(new AgentRequest(
                "请你给我一个详细的用药建议",
                "medical-follow-up",
                "user-1",
                List.of(
                        new AgentMessage("user", "我现在感冒了，你能给我一些建议嘛"),
                        new AgentMessage("assistant", firstResponse.answer())
                ),
                null,
                null,
                false,
                Map.of("channel", "test")
        )));

        assertTrue(secondResponse.answer().contains("年龄段"));
        assertTrue(secondResponse.answer().contains("药物过敏史"));
        assertTrue(secondResponse.answer().contains("正在服用"));
        assertFalse(secondResponse.answer().contains(firstResponse.answer()));
        assertFalse(secondResponse.answer().contains("stub-final-answer:"));
        assertTrue(gateway.prompts.get(1).contains("Do not repeat a previous generic self-care answer"));
        assertTrue(gateway.prompts.get(1).contains("请你给我一个详细的用药建议"));
        assertTrue(gateway.prompts.get(1).contains(firstResponse.answer()));
    }

    private static final class SequenceLocalModelGateway implements LocalModelGateway {

        private final ModelConfig modelConfig;
        private final Queue<String> responses;
        private final List<String> prompts = new ArrayList<>();

        private SequenceLocalModelGateway(ModelConfig modelConfig, List<String> responses) {
            this.modelConfig = modelConfig;
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public String provider() {
            return "sequence-medical-follow-up-test";
        }

        @Override
        public ModelConfig descriptor() {
            return modelConfig;
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            prompts.add(request.prompt());
            String content = responses.remove();
            return new LocalModelResponse(
                    content,
                    request.modelName(),
                    provider(),
                    0L,
                    request.prompt().length(),
                    content.length()
            );
        }
    }
}
