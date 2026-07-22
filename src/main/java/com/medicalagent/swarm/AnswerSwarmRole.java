package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;

import java.util.List;

/** Produces merge constraints only; AgentKernel still owns the final response generation. */
public class AnswerSwarmRole implements SwarmRoleExecutor {

    @Override
    public SwarmRole role() {
        return SwarmRole.ANSWER;
    }

    @Override
    public SwarmRoleResult execute(AgentContext context, SwarmTask task) {
        ObjectNode result = JsonSupport.NODE_FACTORY.objectNode();
        result.put("role", role().name().toLowerCase());
        result.put("status", "ok");
        result.put("finalResponseOwnedBy", "agent_kernel");
        result.put("guardrailRequired", true);
        return new SwarmRoleResult(role(), result, List.of());
    }
}
