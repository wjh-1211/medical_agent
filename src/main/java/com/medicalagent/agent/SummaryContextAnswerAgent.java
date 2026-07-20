package com.medicalagent.agent;

import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class SummaryContextAnswerAgent {

    private static final Logger LOGGER = Logger.getLogger(SummaryContextAnswerAgent.class.getName());
    private static final String TEMPLATE_NAME = "summary-context-answer";
    private static final String CONTINUE_TOOL_NAME = "summary_context_continue";

    private final PromptLoader promptLoader;
    private final PromptVariablesFactory promptVariablesFactory;
    private final PromptRenderer promptRenderer;
    private final LocalModelGateway localModelGateway;
    private final AgentDecisionParser agentDecisionParser;
    private final int modelCallTimeoutMillis;

    public SummaryContextAnswerAgent(
            PromptLoader promptLoader,
            PromptVariablesFactory promptVariablesFactory,
            PromptRenderer promptRenderer,
            LocalModelGateway localModelGateway,
            AgentDecisionParser agentDecisionParser,
            int modelCallTimeoutMillis
    ) {
        this.promptLoader = promptLoader;
        this.promptVariablesFactory = promptVariablesFactory;
        this.promptRenderer = promptRenderer;
        this.localModelGateway = localModelGateway;
        this.agentDecisionParser = agentDecisionParser;
        this.modelCallTimeoutMillis = modelCallTimeoutMillis;
    }

    public Optional<String> answerIfSupported(AgentContext context) {
        if (!context.getToolFacts().path("summaryMemory").path("found").asBoolean(false)) {
            return Optional.empty();
        }
        try {
            PromptTemplate template = promptLoader.load(TEMPLATE_NAME);
            Map<String, String> variables = promptVariablesFactory.create(context, List.of(), List.of());
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
                return Optional.of(decision.answer());
            }
            if (CONTINUE_TOOL_NAME.equals(decision.toolName())) {
                return Optional.empty();
            }
            throw new IllegalArgumentException("Summary context answer returned unsupported tool: " + decision.toolName());
        } catch (RuntimeException exception) {
            LOGGER.warning(() -> "Summary context answer skipped: " + exception.getMessage());
            return Optional.empty();
        }
    }
}
