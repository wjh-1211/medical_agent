package com.medicalagent.swarm;

import java.util.List;

public record SwarmPlan(SwarmPlanMode mode, List<SwarmTask> tasks) {

    public SwarmPlan {
        tasks = List.copyOf(tasks == null ? List.of() : tasks);
    }

    public static SwarmPlan singleAgent() {
        return new SwarmPlan(SwarmPlanMode.SINGLE_AGENT, List.of());
    }
}
