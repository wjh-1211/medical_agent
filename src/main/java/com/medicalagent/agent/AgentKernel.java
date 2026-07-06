package com.medicalagent.agent;

import com.medicalagent.api.AgentResponse;
import com.medicalagent.config.AppConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptTemplate;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;

import java.util.Map;

public record AgentKernel(
        AppConfig config,
        ToolRouter toolRouter,
        PromptLoader promptLoader,
        PromptVariablesFactory promptVariablesFactory,
        PromptRenderer promptRenderer,
        LocalModelGateway localModelGateway
) {

    public AgentResponse handle(AgentContext context) {
        PromptTemplate promptTemplate = promptLoader.load(config.getPrompt().getDefaultTemplate());
        Map<String, String> promptVariables = promptVariablesFactory.create(context);
        String renderedPrompt = promptRenderer.render(promptTemplate, promptVariables);
        LocalModelResponse modelResponse = localModelGateway.generate(new LocalModelRequest(
                renderedPrompt,
                config.getModel().getName(),
                config.getModel().getPath(),
                config.getModel().getTemperature(),
                config.getModel().getMaxTokens(),
                config.getModel().isFunctionCallingEnabled(),
                config.getTimeout().getModelCallMillis()
        ));
        return new AgentResponse(
                "ok",
                context.getRequestId(),
                context.getSessionId(),
                context.getUserId(),
                modelResponse.content(),
                context.getEmergencyFlag(),
                context.getCreatedAt().toString()
        );
    }
}
