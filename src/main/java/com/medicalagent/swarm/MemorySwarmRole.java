package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.LongTermMemoryReadSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.SummaryMemoryReadSkill;

import java.util.ArrayList;
import java.util.List;

/** Reads existing Memory Skills through the Router and returns standard observations. */
public class MemorySwarmRole implements SwarmRoleExecutor {

    private final SkillRegistry skillRegistry;
    private final ToolRouter toolRouter;

    public MemorySwarmRole(SkillRegistry skillRegistry, ToolRouter toolRouter) {
        this.skillRegistry = skillRegistry;
        this.toolRouter = toolRouter;
    }

    @Override
    public SwarmRole role() {
        return SwarmRole.MEMORY;
    }

    @Override
    public SwarmRoleResult execute(AgentContext context, SwarmTask task) {
        List<JsonNode> observations = new ArrayList<>();
        addRead(observations, MemoryReadSkill.TOOL_NAME, input("sessionId", context.getSessionId()));
        addRead(observations, LongTermMemoryReadSkill.TOOL_NAME, input("userId", context.getUserId()));
        addRead(observations, SummaryMemoryReadSkill.TOOL_NAME, input("sessionId", context.getSessionId()));
        ObjectNode result = JsonSupport.NODE_FACTORY.objectNode();
        result.put("role", role().name().toLowerCase());
        result.put("status", "ok");
        result.put("observationCount", observations.size());
        return new SwarmRoleResult(role(), result, observations);
    }

    private ObjectNode input(String key, String value) {
        return JsonSupport.NODE_FACTORY.objectNode().put(key, value);
    }

    private void addRead(List<JsonNode> observations, String toolName, ObjectNode input) {
        if (skillRegistry.findSchemaByToolName(toolName).isEmpty()) {
            return;
        }
        JsonNode result = toolRouter.route(toolName, input);
        observations.add(observation(toolName, input, result));
    }

    private JsonNode observation(String toolName, JsonNode input, JsonNode result) {
        ObjectNode observation = JsonSupport.NODE_FACTORY.objectNode();
        observation.put("toolName", toolName);
        observation.set("input", input.deepCopy());
        observation.set("result", result);
        return observation;
    }
}
