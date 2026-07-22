package com.medicalagent.swarm;

import com.medicalagent.context.AgentContext;

public interface SwarmRoleExecutor {

    SwarmRole role();

    SwarmRoleResult execute(AgentContext context, SwarmTask task);
}
