package com.medicalagent;

import com.medicalagent.agent.AgentKernel;
import com.medicalagent.agent.AgentDecisionParser;
import com.medicalagent.api.AgentController;
import com.medicalagent.api.AgentHttpHandler;
import com.medicalagent.api.AgentHttpServer;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.SessionMemoryStore;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelGatewayRegistry;
import com.medicalagent.model.PythonTransformersLocalModelGatewayFactory;
import com.medicalagent.model.StubLocalModelGatewayFactory;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class MedicalAgentApplication {

    public static void main(String[] args) throws IOException {
        String profile = System.getProperty("app.profile", "local");
        AppConfig appConfig = new ConfigLoader().load(profile);
        SessionMemoryStore sessionMemoryStore = new InMemorySessionMemoryStore(Duration.ofMinutes(appConfig.getMemory().getSessionTtlMinutes()));
        SkillRegistry skillRegistry = new SkillRegistry(appConfig, List.of(
                new EchoSkill(),
                new UppercaseSkill(),
                new MemoryReadSkill(sessionMemoryStore),
                new MemoryWriteSkill(sessionMemoryStore)
        ));
        ToolRouter toolRouter = new ToolRouter(skillRegistry);
        LocalModelGateway localModelGateway = new LocalModelGatewayRegistry(List.of(
                new StubLocalModelGatewayFactory(),
                new PythonTransformersLocalModelGatewayFactory()
        )).create(appConfig);
        localModelGateway.preload();
        Runtime.getRuntime().addShutdownHook(new Thread(localModelGateway::shutdown, "api-local-model-shutdown"));
        AgentKernel agentKernel = new AgentKernel(
                appConfig,
                skillRegistry,
                toolRouter,
                new FilePromptLoader(appConfig.getPrompt()),
                new PromptVariablesFactory(),
                new PromptRenderer(appConfig.getPrompt()),
                localModelGateway,
                new AgentDecisionParser()
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
