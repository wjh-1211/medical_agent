package com.medicalagent.swarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.context.AgentContext;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.KnowledgeSearchSkill;
import com.medicalagent.skills.SkillRegistry;

import java.util.List;

/** Retrieves controlled evidence only through knowledge_search. */
public class RetrieverSwarmRole implements SwarmRoleExecutor {

    private final SkillRegistry skillRegistry;
    private final ToolRouter toolRouter;

    public RetrieverSwarmRole(SkillRegistry skillRegistry, ToolRouter toolRouter) {
        this.skillRegistry = skillRegistry;
        this.toolRouter = toolRouter;
    }

    @Override
    public SwarmRole role() {
        return SwarmRole.RETRIEVER;
    }

    @Override
    public SwarmRoleResult execute(AgentContext context, SwarmTask task) {
        if (skillRegistry.findSchemaByToolName(KnowledgeSearchSkill.TOOL_NAME).isEmpty()) {
            throw new IllegalStateException("Retriever role requires knowledge_search");
        }
        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("query", task.query());
        JsonNode result = toolRouter.route(KnowledgeSearchSkill.TOOL_NAME, input);
        ObjectNode observation = JsonSupport.NODE_FACTORY.objectNode();
        observation.put("toolName", KnowledgeSearchSkill.TOOL_NAME);
        observation.set("input", input);
        observation.set("result", result);
        ObjectNode roleResult = JsonSupport.NODE_FACTORY.objectNode();
        roleResult.put("role", role().name().toLowerCase());
        roleResult.put("status", "ok");
        roleResult.put("found", result.path("found").asBoolean(false));
        roleResult.set("evidence", result.path("chunks").deepCopy());
        return new SwarmRoleResult(role(), roleResult, List.of(observation));
    }
}
