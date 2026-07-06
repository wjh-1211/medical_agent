package com.medicalagent.api;

import com.medicalagent.agent.AgentKernel;
import com.medicalagent.context.AgentContext;
import com.medicalagent.context.AgentContextFactory;

public class AgentController {

    private final AgentContextFactory contextFactory;
    private final AgentKernel agentKernel;

    public AgentController(AgentContextFactory contextFactory, AgentKernel agentKernel) {
        this.contextFactory = contextFactory;
        this.agentKernel = agentKernel;
    }

    public AgentResponse handle(AgentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be null");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("request.message must not be blank");
        }
        AgentContext context = contextFactory.create(request);
        return agentKernel.handle(context);
    }
}
