package com.medicalagent.api;

import com.medicalagent.config.ApiConfig;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AgentHttpServer {

    private final HttpServer server;
    private final ApiConfig apiConfig;

    public AgentHttpServer(ApiConfig apiConfig, AgentHttpHandler httpHandler) throws IOException {
        this.apiConfig = apiConfig;
        this.server = HttpServer.create(new InetSocketAddress(apiConfig.getHost(), apiConfig.getPort()), 0);
        this.server.createContext(apiConfig.getBasePath(), httpHandler);
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    public String endpoint() {
        return "http://" + apiConfig.getHost() + ":" + apiConfig.getPort() + apiConfig.getBasePath();
    }
}
