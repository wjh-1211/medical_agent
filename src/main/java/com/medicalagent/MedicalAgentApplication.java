package com.medicalagent;

import com.medicalagent.agent.AgentKernel;
import com.medicalagent.agent.AgentDecisionParser;
import com.medicalagent.api.AgentController;
import com.medicalagent.api.AgentHttpHandler;
import com.medicalagent.api.AgentHttpServer;
import com.medicalagent.config.AppConfig;
import com.medicalagent.config.ConfigLoader;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.knowledge.KnowledgeService;
import com.medicalagent.knowledge.KnowledgeServiceFactory;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.memory.LongTermMemoryStore;
import com.medicalagent.memory.LongTermMemoryStoreFactory;
import com.medicalagent.memory.SessionMemoryStore;
import com.medicalagent.memory.SummaryMemoryStore;
import com.medicalagent.memory.SummaryMemoryStoreFactory;
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
import com.medicalagent.skills.LongTermMemoryReadSkill;
import com.medicalagent.skills.LongTermMemoryWriteSkill;
import com.medicalagent.skills.KnowledgeSearchSkill;
import com.medicalagent.skills.SummaryMemoryReadSkill;
import com.medicalagent.skills.SummaryMemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MedicalAgentApplication {

    public static void main(String[] args) throws IOException {
        String profile = System.getProperty("app.profile", "local");
        AppConfig appConfig = new ConfigLoader().load(profile);
        SessionMemoryStore sessionMemoryStore = new InMemorySessionMemoryStore(Duration.ofMinutes(appConfig.getMemory().getSessionTtlMinutes()));
        LongTermMemoryStore longTermMemoryStore = new LongTermMemoryStoreFactory().create(appConfig.getMemory());
        SummaryMemoryStore summaryMemoryStore = new SummaryMemoryStoreFactory().create(appConfig.getMemory());
        List<com.medicalagent.skills.Skill> skills = new ArrayList<>(List.of(
                new EchoSkill(),
                new UppercaseSkill(),
                new MemoryReadSkill(sessionMemoryStore),
                new MemoryWriteSkill(sessionMemoryStore),
                new LongTermMemoryReadSkill(longTermMemoryStore),
                new LongTermMemoryWriteSkill(longTermMemoryStore),
                new SummaryMemoryReadSkill(summaryMemoryStore),
                new SummaryMemoryWriteSkill(summaryMemoryStore)
        ));
        KnowledgeService knowledgeService = new KnowledgeServiceFactory()
                .create(appConfig.getKnowledge())
                .orElse(null);
        if (knowledgeService != null) {
            knowledgeService.rebuildIndex();
            skills.add(new KnowledgeSearchSkill(knowledgeService, appConfig.getKnowledge().getDefaultTopK()));
            Runtime.getRuntime().addShutdownHook(new Thread(knowledgeService::close, "api-knowledge-embedding-shutdown"));
        }
        SkillRegistry skillRegistry = new SkillRegistry(appConfig, skills);
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
                new PromptVariablesFactory(appConfig.getContext()),
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
