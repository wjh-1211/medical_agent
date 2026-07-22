package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.context.AgentContext;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.prompt.PromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.tracing.TraceEventType;
import com.medicalagent.tracing.TraceScope;
import com.medicalagent.tracing.TraceSink;
import com.medicalagent.tracing.TraceStatus;
import com.medicalagent.tracing.TraceSupport;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Bounded sequential orchestrator that falls back to the existing single-Agent path. */
public class SwarmOrchestrator {

    private final AppConfig config;
    private final SwarmPlanner planner;
    private final Map<SwarmRole, SwarmRoleExecutor> executors;
    private final TraceSink traceSink;

    public SwarmOrchestrator(
            AppConfig config,
            SkillRegistry skillRegistry,
            ToolRouter toolRouter,
            PromptLoader promptLoader,
            PromptVariablesFactory promptVariablesFactory,
            PromptRenderer promptRenderer,
            LocalModelGateway localModelGateway,
            TraceSink traceSink
    ) {
        this.config = config;
        this.planner = new SwarmPlanner(
                config, promptLoader, promptVariablesFactory, promptRenderer, localModelGateway, new SwarmPlanParser()
        );
        this.executors = executors(skillRegistry, toolRouter);
        this.traceSink = traceSink;
    }

    public SwarmOutcome orchestrate(AgentContext context) {
        if (!config.getSwarm().isEnabled()) {
            return SwarmOutcome.singleAgent("swarm_disabled");
        }
        String traceId = TraceScope.currentTraceId().orElse(context.getRequestId());
        long planStartedAt = System.nanoTime();
        final SwarmPlan plan;
        try {
            plan = planner.plan(context);
            record(traceId, TraceEventType.SWARM_PLAN, "swarm.planner", TraceStatus.SUCCEEDED,
                    elapsedMillis(planStartedAt), null, context.getMessage(), planSummary(plan), Map.of("mode", plan.mode().name()));
        } catch (RuntimeException exception) {
            return fallback(traceId, "planner_failed", exception, planStartedAt);
        }
        if (plan.mode() == SwarmPlanMode.SINGLE_AGENT) {
            record(traceId, TraceEventType.SWARM_FALLBACK, "swarm.single_agent", TraceStatus.SUCCEEDED,
                    0L, null, "", "", Map.of("reason", "planner_selected_single_agent"));
            return SwarmOutcome.singleAgent("planner_selected_single_agent");
        }

        var results = new java.util.ArrayList<SwarmRoleResult>();
        var observations = new java.util.ArrayList<JsonNode>();
        for (SwarmTask task : plan.tasks()) {
            SwarmRoleExecutor executor = executors.get(task.role());
            if (executor == null) {
                return fallback(traceId, "missing_role_executor", new IllegalStateException(task.role().name()), planStartedAt);
            }
            long roleStartedAt = System.nanoTime();
            try {
                SwarmRoleResult result = executor.execute(context, task);
                long elapsedMillis = elapsedMillis(roleStartedAt);
                if (elapsedMillis > config.getSwarm().getRoleTimeoutMillis()) {
                    return fallback(traceId, "role_timeout", new IllegalStateException(task.role().name()), roleStartedAt);
                }
                results.add(result);
                observations.addAll(result.observations());
                record(traceId, TraceEventType.SWARM_ROLE, "swarm.role." + task.role().name().toLowerCase(), TraceStatus.SUCCEEDED,
                        elapsedMillis, null, task.query(), TraceSupport.summarizeJson(result.result(), config.getTracing().getMaxPayloadCharacters()), Map.of());
            } catch (RuntimeException exception) {
                return fallback(traceId, "role_failed", exception, roleStartedAt);
            }
        }
        JsonNode merged = merge(results);
        record(traceId, TraceEventType.SWARM_MERGE, "swarm.merge", TraceStatus.SUCCEEDED, 0L,
                null, "", TraceSupport.summarizeJson(merged, config.getTracing().getMaxPayloadCharacters()),
                Map.of("roleCount", Integer.toString(results.size())));
        observations.add(mergeObservation(merged));
        return new SwarmOutcome(true, results, observations, "");
    }

    private Map<SwarmRole, SwarmRoleExecutor> executors(SkillRegistry skillRegistry, ToolRouter toolRouter) {
        Map<SwarmRole, SwarmRoleExecutor> configured = new EnumMap<>(SwarmRole.class);
        List<SwarmRoleExecutor> all = List.of(
                new MemorySwarmRole(skillRegistry, toolRouter),
                new RetrieverSwarmRole(skillRegistry, toolRouter),
                new SafetySwarmRole(),
                new AnswerSwarmRole()
        );
        all.forEach(executor -> configured.put(executor.role(), executor));
        return Map.copyOf(configured);
    }

    private SwarmOutcome fallback(String traceId, String reason, RuntimeException exception, long startedAt) {
        record(traceId, TraceEventType.SWARM_FALLBACK, "swarm.fallback", TraceStatus.FAILED,
                elapsedMillis(startedAt), exception, "", "", Map.of("reason", reason));
        if (!config.getSwarm().isFallbackToSingleAgent()) {
            throw exception;
        }
        return SwarmOutcome.singleAgent(reason);
    }

    private JsonNode merge(List<SwarmRoleResult> results) {
        ObjectNode merged = JsonSupport.NODE_FACTORY.objectNode();
        merged.put("status", "ok");
        merged.put("roleCount", results.size());
        ArrayNode roles = merged.putArray("roles");
        for (SwarmRoleResult result : results) {
            ObjectNode role = roles.addObject();
            role.put("role", result.role().name().toLowerCase());
            role.set("result", result.result());
        }
        return merged;
    }

    private JsonNode mergeObservation(JsonNode merged) {
        ObjectNode observation = JsonSupport.NODE_FACTORY.objectNode();
        observation.put("toolName", "swarm_merge");
        observation.set("input", JsonSupport.NODE_FACTORY.objectNode());
        observation.set("result", merged);
        return observation;
    }

    private String planSummary(SwarmPlan plan) {
        return plan.mode().name() + ":" + plan.tasks().stream().map(task -> task.role().name()).toList();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private void record(
            String traceId,
            TraceEventType type,
            String name,
            TraceStatus status,
            long durationMillis,
            RuntimeException error,
            String input,
            String output,
            Map<String, String> attributes
    ) {
        traceSink.record(TraceSupport.event(traceId, type, name, status, durationMillis, error, input, output,
                attributes, config.getTracing().getMaxPayloadCharacters()));
    }
}
