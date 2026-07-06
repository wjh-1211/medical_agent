package com.medicalagent.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.medicalagent.common.JsonSupport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class AgentHttpHandler implements HttpHandler {

    private final AgentController controller;

    public AgentHttpHandler(AgentController controller) {
        this.controller = controller;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, Map.of("status", "error", "message", "Only POST is supported"));
                return;
            }

            AgentRequest request;
            try (InputStream requestBody = exchange.getRequestBody()) {
                request = JsonSupport.JSON_MAPPER.readValue(requestBody, AgentRequest.class);
            } catch (JsonProcessingException exception) {
                writeJson(exchange, 400, Map.of("status", "error", "message", "Invalid JSON request body"));
                return;
            }

            AgentResponse response = controller.handle(request);
            writeJson(exchange, 200, response);
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, Map.of("status", "error", "message", exception.getMessage()));
        } catch (Exception exception) {
            writeJson(exchange, 500, Map.of("status", "error", "message", "Internal server error"));
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = JsonSupport.JSON_MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
            outputStream.flush();
        }
    }
}
