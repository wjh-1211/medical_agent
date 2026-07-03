package com.medicalagent.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;

public class EchoSkill implements Skill {

    @Override
    public String id() {
        return "echoSkill";
    }

    @Override
    public ToolSpec tool() {
        return new ToolSpec(
                "echo_input",
                "Return the received payload as structured observation for router integration tests.",
                """
                {
                  "type": "object",
                  "properties": {
                    "message": { "type": "string" }
                  },
                  "required": ["message"]
                }
                """
        );
    }

    @Override
    public JsonNode execute(JsonNode input) {
        ObjectNode output = JsonSupport.NODE_FACTORY.objectNode();
        output.put("skill", id());
        output.set("input", input);
        output.put("status", "ok");
        return output;
    }
}
