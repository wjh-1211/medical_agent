package com.medicalagent.agent;

import com.medicalagent.config.AppConfig;
import com.medicalagent.runtime.ToolRouter;

public record AgentKernel(
        AppConfig config,
        ToolRouter toolRouter
) {
}
