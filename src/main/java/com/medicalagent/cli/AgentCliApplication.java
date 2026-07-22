package com.medicalagent.cli;

import com.medicalagent.agent.AgentDecisionParser;
import com.medicalagent.agent.AgentKernel;
import com.medicalagent.api.AgentController;
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
import com.medicalagent.tracing.TraceRuntime;
import com.medicalagent.tracing.TraceRuntimeFactory;
import com.medicalagent.tracing.TracingLocalModelGateway;
import com.medicalagent.skills.EchoSkill;
import com.medicalagent.skills.EmergencyDetectionSkill;
import com.medicalagent.skills.GroundingCheckSkill;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;
import com.medicalagent.skills.RiskAssessmentSkill;
import com.medicalagent.skills.LongTermMemoryReadSkill;
import com.medicalagent.skills.LongTermMemoryWriteSkill;
import com.medicalagent.skills.KnowledgeSearchSkill;
import com.medicalagent.skills.SummaryMemoryReadSkill;
import com.medicalagent.skills.SummaryMemoryWriteSkill;
import com.medicalagent.skills.SkillRegistry;
import com.medicalagent.skills.UppercaseSkill;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AgentCliApplication {

    public static void main(String[] args) {
        String profile = System.getProperty("app.profile", "local");
        AppConfig appConfig = new ConfigLoader().load(profile);
        TraceRuntime traceRuntime = new TraceRuntimeFactory().create(appConfig.getTracing());
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
                new SummaryMemoryWriteSkill(summaryMemoryStore),
                new EmergencyDetectionSkill(),
                new RiskAssessmentSkill(),
                new GroundingCheckSkill()
        ));
        KnowledgeService knowledgeService = new KnowledgeServiceFactory()
                .create(appConfig.getKnowledge())
                .orElse(null);
        if (knowledgeService != null) {
            knowledgeService.rebuildIndex();
            skills.add(new KnowledgeSearchSkill(knowledgeService, appConfig.getKnowledge().getDefaultTopK()));
            Runtime.getRuntime().addShutdownHook(new Thread(knowledgeService::close, "cli-knowledge-embedding-shutdown"));
        }
        SkillRegistry skillRegistry = new SkillRegistry(appConfig, skills);
        ToolRouter toolRouter = new ToolRouter(skillRegistry, traceRuntime.sink(),
                appConfig.getTracing().getMaxPayloadCharacters(), appConfig.getTracing().getSlowCallMillis());
        LocalModelGateway localModelGateway = new TracingLocalModelGateway(new LocalModelGatewayRegistry(List.of(
                new StubLocalModelGatewayFactory(),
                new PythonTransformersLocalModelGatewayFactory()
        )).create(appConfig), traceRuntime.sink(), appConfig.getTracing().getMaxPayloadCharacters(),
                appConfig.getTracing().getSlowCallMillis());
        SystemCliConsole console = new SystemCliConsole();
        if ("python_transformers".equals(localModelGateway.provider())) {
            System.out.println("Agent> 本地模型启动中...");
            localModelGateway.preload(new CliStartupLoadingListener(console));
            Runtime.getRuntime().addShutdownHook(new Thread(localModelGateway::shutdown, "cli-local-model-shutdown"));
        }

        AgentKernel agentKernel = new AgentKernel(
                appConfig,
                skillRegistry,
                toolRouter,
                new FilePromptLoader(appConfig.getPrompt()),
                new PromptVariablesFactory(appConfig.getContext()),
                new PromptRenderer(appConfig.getPrompt()),
                localModelGateway,
                new AgentDecisionParser(),
                traceRuntime.sink()
        );
        AgentController controller = new AgentController(new AgentContextFactory(appConfig), agentKernel);

        AgentCliRunner runner = new AgentCliRunner(
                console,
                new CliCommandParser(),
                new AgentCliSession("cli-user"),
                controller::handle
        );
        runner.run();
    }
}
