package com.medicalagent.cli;

import com.medicalagent.agent.AgentDecisionParser;
import com.medicalagent.agent.AgentKernel;
import com.medicalagent.api.AgentController;
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
import com.medicalagent.skills.UppercaseSkill;

import java.util.List;

public class AgentCliApplication {

    public static void main(String[] args) {
        String profile = System.getProperty("app.profile", "local");
        AppConfig appConfig = new ConfigLoader().load(profile);
        SkillRegistry skillRegistry = new SkillRegistry(appConfig, List.of(new EchoSkill(), new UppercaseSkill()));
        ToolRouter toolRouter = new ToolRouter(skillRegistry);
        LocalModelGateway localModelGateway = new LocalModelGatewayRegistry(List.of(
                new StubLocalModelGatewayFactory(),
                new PythonTransformersLocalModelGatewayFactory()
        )).create(appConfig);

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
        AgentController controller = new AgentController(new AgentContextFactory(appConfig), agentKernel);

        AgentCliRunner runner = new AgentCliRunner(
                new SystemCliConsole(),
                new CliCommandParser(),
                new AgentCliSession("cli-user"),
                controller::handle
        );
        runner.run();
    }
}
