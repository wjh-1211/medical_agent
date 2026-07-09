package com.medicalagent.agent;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.config.ModelConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentKernelReActTest {

    @Test
    void shouldFailWhenModelResponseIsNotStructuredJson() {
        AppConfig config = new ConfigLoader().load("test");
        AgentKernel kernel = createKernel(config, new SequenceLocalModelGateway(config.getModel(), List.of("not-json")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> kernel.handle(createContext(config))
        );

        assertEquals("Model response must be valid JSON: not-json", exception.getMessage());
    }

    @Test
    void shouldFailWhenReActLoopDoesNotReachFinalAnswer() {
        AppConfig config = new ConfigLoader().load("test");
        config.getRuntime().setMaxReActLoops(1);
        AgentKernel kernel = createKernel(config, new SequenceLocalModelGateway(config.getModel(), List.of("""
                {
                  "type": "tool_call",
                  "toolName": "uppercase_text",
                  "input": {
                    "message": "Need help"
                  }
                }
                """)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> kernel.handle(createContext(config))
        );

        assertEquals("Agent did not reach final_answer within maxReActLoops=1", exception.getMessage());
    }

    @Test
    void shouldFailWhenToolCallReferencesUnknownTool() {
        AppConfig config = new ConfigLoader().load("test");
        AgentKernel kernel = createKernel(config, new SequenceLocalModelGateway(config.getModel(), List.of("""
                {
                  "type": "tool_call",
                  "toolName": "missing_tool",
                  "input": {
                    "message": "Need help"
                  }
                }
                """)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> kernel.handle(createContext(config))
        );

        assertEquals("Tool execution failed for tool: missing_tool", exception.getMessage());
    }

    private AgentKernel createKernel(AppConfig config, LocalModelGateway localModelGateway) {
        SkillRegistry registry = new SkillRegistry(config, List.of(new EchoSkill(), new UppercaseSkill()));
        return new AgentKernel(
                config,
                registry,
                new ToolRouter(registry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(config.getPrompt()),
                localModelGateway,
                new AgentDecisionParser()
        );
    }

    private AgentContext createContext(AppConfig config) {
        return new AgentContextFactory(config).create(new AgentRequest(
                "Need help",
                "session-42",
                "user-7",
                List.of(),
                null,
                null,
                null,
                Map.of()
        ));
    }

    private static final class SequenceLocalModelGateway implements LocalModelGateway {

        private final ModelConfig modelConfig;
        private final Queue<String> responses;

        private SequenceLocalModelGateway(ModelConfig modelConfig, List<String> responses) {
            this.modelConfig = modelConfig;
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public String provider() {
            return "sequence-test";
        }

        @Override
        public ModelConfig descriptor() {
            return modelConfig;
        }

        @Override
        public LocalModelResponse generate(LocalModelRequest request) {
            String content = responses.isEmpty() ? """
                    {
                      "type": "final_answer",
                      "answer": "fallback"
                    }
                    """ : responses.remove();
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
