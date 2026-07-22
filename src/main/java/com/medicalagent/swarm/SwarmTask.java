package com.medicalagent.swarm;

public record SwarmTask(SwarmRole role, String query) {

    public SwarmTask {
        query = query == null ? "" : query.trim();
    }
}
