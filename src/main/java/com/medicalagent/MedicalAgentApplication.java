package com.medicalagent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.SkillRegistry;

import java.util.List;

public class MedicalAgentApplication {

    public static void main(String[] args) {
        String profile = System.getProperty("app.profile", "local");
        AppConfig appConfig = new ConfigLoader().load(profile);
        SkillRegistry skillRegistry = new SkillRegistry(appConfig, List.of(new EchoSkill()));
        ToolRouter toolRouter = new ToolRouter(skillRegistry);

        ObjectNode input = JsonSupport.NODE_FACTORY.objectNode();
        input.put("message", "framework bootstrapped");

        System.out.println("Loaded profile: " + appConfig.getRuntime().getEnvironment());
        System.out.println(toolRouter.route("echo_input", input).toPrettyString());
    }
}
