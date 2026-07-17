package com.medicalagent.agent;

import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.LongTermMemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.ToolSchema;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LongTermMemoryUpdateAgent {

    private static final Logger LOGGER = Logger.getLogger(LongTermMemoryUpdateAgent.class.getName());
    private static final String TEMPLATE_NAME = "long-term-memory-update";

    private final SkillRegistry skillRegistry;
    private final ToolRouter toolRouter;
    private final PromptLoader promptLoader;
    private final PromptVariablesFactory promptVariablesFactory;
    private final PromptRenderer promptRenderer;
    private final LocalModelGateway localModelGateway;
    private final AgentDecisionParser agentDecisionParser;
    private final int modelCallTimeoutMillis;

    public LongTermMemoryUpdateAgent(
            SkillRegistry skillRegistry,
            ToolRouter toolRouter,
            PromptLoader promptLoader,
            PromptVariablesFactory promptVariablesFactory,
            PromptRenderer promptRenderer,
            LocalModelGateway localModelGateway,
            AgentDecisionParser agentDecisionParser,
            int modelCallTimeoutMillis
    ) {
        this.skillRegistry = skillRegistry;
        this.toolRouter = toolRouter;
        this.promptLoader = promptLoader;
        this.promptVariablesFactory = promptVariablesFactory;
        this.promptRenderer = promptRenderer;
        this.localModelGateway = localModelGateway;
        this.agentDecisionParser = agentDecisionParser;
        this.modelCallTimeoutMillis = modelCallTimeoutMillis;
    }

    public void update(AgentContext context, String agentAnswer) {
        ToolSchema writeSchema = skillRegistry.findSchemaByToolName(LongTermMemoryWriteSkill.TOOL_NAME).orElse(null);
        if (writeSchema == null) {
            return;
        }
        try {
            PromptTemplate template = promptLoader.load(TEMPLATE_NAME);
            Map<String, String> variables = promptVariablesFactory.create(context, List.of(writeSchema), List.of());
            variables.put("agentAnswer", agentAnswer);
            String prompt = promptRenderer.render(template, variables);
            String content = localModelGateway.generate(new LocalModelRequest(
                    prompt,
                    localModelGateway.descriptor().getName(),
                    localModelGateway.descriptor().getPath(),
                    localModelGateway.descriptor().getTemperature(),
                    localModelGateway.descriptor().getMaxTokens(),
                    true,
                    modelCallTimeoutMillis
            )).content();
            AgentDecision decision = agentDecisionParser.parse(content);
            if (decision.type() == AgentDecision.Type.FINAL_ANSWER) {
                return;
            }
            if (!LongTermMemoryWriteSkill.TOOL_NAME.equals(decision.toolName())) {
                throw new IllegalArgumentException("Long-term memory update returned unsupported tool: " + decision.toolName());
            }
            toolRouter.route(decision.toolName(), decision.input());
        } catch (RuntimeException exception) {
            LOGGER.warning(() -> "Long-term memory update skipped: " + exception.getMessage());
        }
    }
}
