package com.medicalagent.swarm;

import com.medicalagent.config.AppConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;

import java.util.Map;

/** LLM-only planner that returns a bounded Swarm JSON plan. */
public class SwarmPlanner {

    private static final String TEMPLATE_NAME = "swarm-planner";

    private final AppConfig config;
    private final PromptLoader promptLoader;
    private final PromptVariablesFactory promptVariablesFactory;
    private final PromptRenderer promptRenderer;
    private final LocalModelGateway localModelGateway;
    private final SwarmPlanParser planParser;

    public SwarmPlanner(
            AppConfig config,
            PromptLoader promptLoader,
            PromptVariablesFactory promptVariablesFactory,
            PromptRenderer promptRenderer,
            LocalModelGateway localModelGateway,
            SwarmPlanParser planParser
    ) {
        this.config = config;
        this.promptLoader = promptLoader;
        this.promptVariablesFactory = promptVariablesFactory;
        this.promptRenderer = promptRenderer;
        this.localModelGateway = localModelGateway;
        this.planParser = planParser;
    }

    public SwarmPlan plan(AgentContext context) {
        PromptTemplate template = promptLoader.load(TEMPLATE_NAME);
        Map<String, String> variables = promptVariablesFactory.create(context);
        variables.put("maxRoles", Integer.toString(config.getSwarm().getMaxRoles()));
        variables.put("maxPlanSteps", Integer.toString(config.getSwarm().getMaxPlanSteps()));
        String prompt = promptRenderer.render(template, variables);
        String content = localModelGateway.generate(new LocalModelRequest(
                prompt,
                localModelGateway.descriptor().getName(),
                localModelGateway.descriptor().getPath(),
                localModelGateway.descriptor().getTemperature(),
                localModelGateway.descriptor().getMaxTokens(),
                true,
                config.getTimeout().getModelCallMillis()
        )).content();
        return planParser.parse(content, config.getSwarm());
    }
}
