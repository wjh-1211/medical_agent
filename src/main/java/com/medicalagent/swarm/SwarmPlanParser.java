package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.SwarmConfig;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class SwarmPlanParser {

    public SwarmPlan parse(String rawPlan, SwarmConfig config) {
        try {
            JsonNode root = JsonSupport.JSON_MAPPER.readTree(rawPlan);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Swarm plan must be a JSON object");
            }
            String mode = root.path("mode").asText().trim().toLowerCase();
            if ("single_agent".equals(mode)) {
                return SwarmPlan.singleAgent();
            }
            if (!"swarm".equals(mode)) {
                throw new IllegalArgumentException("Unsupported swarm plan mode: " + mode);
            }
            if (!root.path("tasks").isArray()) {
                throw new IllegalArgumentException("Swarm plan tasks must be an array");
            }
            List<SwarmTask> tasks = new ArrayList<>();
            Set<SwarmRole> roles = EnumSet.noneOf(SwarmRole.class);
            for (JsonNode task : root.path("tasks")) {
                if (!task.isObject()) {
                    throw new IllegalArgumentException("Swarm task must be a JSON object");
                }
                if (tasks.size() >= config.getMaxPlanSteps()) {
                    throw new IllegalArgumentException("Swarm plan exceeds maxPlanSteps");
                }
                SwarmRole role = SwarmRole.valueOf(task.path("role").asText().trim().toUpperCase());
                if (!roles.add(role)) {
                    throw new IllegalArgumentException("Swarm plan must not repeat role: " + role.name().toLowerCase());
                }
                String query = task.path("query").asText();
                if (role == SwarmRole.RETRIEVER && query.isBlank()) {
                    throw new IllegalArgumentException("Retriever swarm task requires a query");
                }
                tasks.add(new SwarmTask(role, query));
            }
            if (tasks.size() < 2 || tasks.size() > config.getMaxRoles()) {
                throw new IllegalArgumentException("Swarm plan must contain between 2 and maxRoles tasks");
            }
            return new SwarmPlan(SwarmPlanMode.SWARM, tasks);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid swarm plan: " + exception.getMessage(), exception);
        }
    }
}
