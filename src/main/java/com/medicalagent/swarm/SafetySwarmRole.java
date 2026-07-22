package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;

import java.util.List;

/** Supplies a non-final safety constraint; the Guardrail Pipeline remains authoritative. */
public class SafetySwarmRole implements SwarmRoleExecutor {

    @Override
    public SwarmRole role() {
        return SwarmRole.SAFETY;
    }

    @Override
    public SwarmRoleResult execute(AgentContext context, SwarmTask task) {
        ObjectNode result = JsonSupport.NODE_FACTORY.objectNode();
        result.put("role", role().name().toLowerCase());
        result.put("status", "ok");
        result.put("guardrailRequired", true);
        result.put("requestEmergencyFlag", Boolean.TRUE.equals(context.getEmergencyFlag()));
        result.put("instruction", "Do not return a final user answer from this role; existing Guardrail Pipeline remains mandatory.");
        return new SwarmRoleResult(role(), result, List.of());
    }
}
