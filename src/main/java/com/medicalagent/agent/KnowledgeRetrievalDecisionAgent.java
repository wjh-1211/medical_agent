package com.medicalagent.agent;

import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.skills.ToolSchema;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/** LLM-only preflight for deciding whether controlled external evidence is needed. */
public class KnowledgeRetrievalDecisionAgent {

    private static final Logger LOGGER = Logger.getLogger(KnowledgeRetrievalDecisionAgent.class.getName());
    private static final String TEMPLATE_NAME = "knowledge-retrieval-decision";
    private static final String CONTINUE_TOOL_NAME = "knowledge_retrieval_continue";

    private final PromptLoader promptLoader;
    private final PromptVariablesFactory promptVariablesFactory;
    private final PromptRenderer promptRenderer;
    private final LocalModelGateway localModelGateway;
    private final AgentDecisionParser agentDecisionParser;
    private final int modelCallTimeoutMillis;

    public KnowledgeRetrievalDecisionAgent(
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

    public Optional<AgentDecision> decide(AgentContext context, ToolSchema knowledgeSearchSchema) {
        try {
            PromptTemplate template = promptLoader.load(TEMPLATE_NAME);
            Map<String, String> variables = promptVariablesFactory.create(context, List.of(knowledgeSearchSchema), List.of());
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
            if (decision.type() == AgentDecision.Type.TOOL_CALL
                    && knowledgeSearchSchema.name().equals(decision.toolName())) {
                return Optional.of(decision);
            }
            if (decision.type() == AgentDecision.Type.TOOL_CALL && CONTINUE_TOOL_NAME.equals(decision.toolName())) {
                return Optional.empty();
            }
            throw new IllegalArgumentException("Knowledge retrieval decision returned unsupported result");
        } catch (RuntimeException exception) {
            LOGGER.warning(() -> "Knowledge retrieval preflight skipped: " + exception.getMessage());
            return Optional.empty();
        }
    }
}
