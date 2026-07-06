package com.medicalagent;

import com.medicalagent.agent.AgentKernel;
import com.medicalagent.api.AgentController;
import com.medicalagent.api.AgentHttpHandler;
import com.medicalagent.api.AgentHttpServer;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelGatewayRegistry;
import com.medicalagent.model.PythonTransformersLocalModelGatewayFactory;
import com.medicalagent.model.StubLocalModelGatewayFactory;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.SkillRegistry;

import java.io.IOException;
import java.util.List;

public class MedicalAgentApplication {

    public static void main(String[] args) throws IOException {
        String profile = System.getProperty("app.profile", "local");
        AppConfig appConfig = new ConfigLoader().load(profile);
        SkillRegistry skillRegistry = new SkillRegistry(appConfig, List.of(new EchoSkill()));
        ToolRouter toolRouter = new ToolRouter(skillRegistry);
        LocalModelGateway localModelGateway = new LocalModelGatewayRegistry(List.of(
                new StubLocalModelGatewayFactory(),
                new PythonTransformersLocalModelGatewayFactory()
        )).create(appConfig);
        AgentKernel agentKernel = new AgentKernel(
                appConfig,
                toolRouter,
                new FilePromptLoader(appConfig.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(appConfig.getPrompt()),
                localModelGateway
        );
        AgentContextFactory contextFactory = new AgentContextFactory(appConfig);
        AgentController controller = new AgentController(contextFactory, agentKernel);

        if (!appConfig.getApi().isEnabled()) {
            System.out.println("API disabled for profile: " + profile);
            return;
        }

        AgentHttpServer server = new AgentHttpServer(appConfig.getApi(), new AgentHttpHandler(controller));
        server.start();
        System.out.println("Loaded profile: " + appConfig.getRuntime().getEnvironment());
        System.out.println("Agent API started at " + server.endpoint());
    }
}
