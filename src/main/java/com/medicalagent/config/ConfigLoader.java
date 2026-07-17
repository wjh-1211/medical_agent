package com.medicalagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class ConfigLoader {

    public AppConfig load(String profile) {
        String resolvedProfile = (profile == null || profile.isBlank()) ? "local" : profile;
        JsonNode baseNode = readYaml("config/application.yml");
        JsonNode profileNode = readYaml("config/application-" + resolvedProfile + ".yml");
        JsonNode merged = deepMerge(baseNode.deepCopy(), profileNode);
        AppConfig config = JsonSupport.YAML_MAPPER.convertValue(merged, AppConfig.class);
        validate(config);
        return config;
    }

    private JsonNode readYaml(String resourcePath) {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ConfigException("Missing config resource: " + resourcePath);
            }
            return JsonSupport.YAML_MAPPER.readTree(inputStream);
        } catch (IOException exception) {
            throw new ConfigException("Failed to read config resource: " + resourcePath, exception);
        }
    }

    private JsonNode deepMerge(JsonNode baseNode, JsonNode overrideNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = overrideNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode baseChild = baseNode.get(field.getKey());
            JsonNode overrideChild = field.getValue();
            if (baseChild != null && baseChild.isObject() && overrideChild.isObject()) {
                deepMerge(baseChild, overrideChild);
            } else {
                ((com.fasterxml.jackson.databind.node.ObjectNode) baseNode).set(field.getKey(), overrideChild);
            }
        }
        return baseNode;
    }

    void validate(AppConfig config) {
        if (config.getRuntime().getMaxReActLoops() <= 0) {
            throw new ConfigException("runtime.maxReActLoops must be greater than 0");
        }
        if (config.getApi().getPort() <= 0) {
            throw new ConfigException("api.port must be greater than 0");
        }
        if (config.getApi().getBasePath() == null
                || config.getApi().getBasePath().isBlank()
                || !config.getApi().getBasePath().startsWith("/")) {
            throw new ConfigException("api.basePath must start with '/'");
        }
        if (config.getApi().getRequestTimeoutMillis() <= 0) {
            throw new ConfigException("api.requestTimeoutMillis must be greater than 0");
        }
        if (config.getPrompt().getDirectory() == null || config.getPrompt().getDirectory().isBlank()) {
            throw new ConfigException("prompt.directory must not be blank");
        }
        if (config.getPrompt().getDefaultTemplate() == null || config.getPrompt().getDefaultTemplate().isBlank()) {
            throw new ConfigException("prompt.defaultTemplate must not be blank");
        }
        if (config.getPrompt().getFileExtension() == null || config.getPrompt().getFileExtension().isBlank()) {
            throw new ConfigException("prompt.fileExtension must not be blank");
        }
        if (config.getModel().getProvider() == null || config.getModel().getProvider().isBlank()) {
            throw new ConfigException("model.provider must not be blank");
        }
        if (config.getModel().getName() == null || config.getModel().getName().isBlank()) {
            throw new ConfigException("model.name must not be blank");
        }
        if (config.getModel().getMaxTokens() <= 0) {
            throw new ConfigException("model.maxTokens must be greater than 0");
        }
        if (config.getModel().getProvider().equals("python_transformers")
                && (config.getModel().getPath() == null || config.getModel().getPath().isBlank())) {
            throw new ConfigException("model.path must not be blank when provider is python_transformers");
        }
        if (config.getModel().getPythonExecutable() == null || config.getModel().getPythonExecutable().isBlank()) {
            throw new ConfigException("model.pythonExecutable must not be blank");
        }
        if (config.getModel().getLauncherScript() == null || config.getModel().getLauncherScript().isBlank()) {
            throw new ConfigException("model.launcherScript must not be blank");
        }
        if (config.getSession().isAllowAnonymousUser()
                && (config.getSession().getAnonymousUserIdPrefix() == null
                || config.getSession().getAnonymousUserIdPrefix().isBlank())) {
            throw new ConfigException("session.anonymousUserIdPrefix must not be blank");
        }
        if (config.getMemory().getLongTermStore() == null || config.getMemory().getLongTermStore().isBlank()) {
            throw new ConfigException("memory.longTermStore must not be blank");
        }
        if (config.getMemory().getLongTermSqlitePath() == null || config.getMemory().getLongTermSqlitePath().isBlank()) {
            throw new ConfigException("memory.longTermSqlitePath must not be blank");
        }
        if (config.getTimeout().getModelCallMillis() <= 0) {
            throw new ConfigException("timeout.modelCallMillis must be greater than 0");
        }
    }
}
