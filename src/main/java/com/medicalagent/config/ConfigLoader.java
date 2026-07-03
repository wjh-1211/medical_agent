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
        if (config.getModel().getName() == null || config.getModel().getName().isBlank()) {
            throw new ConfigException("model.name must not be blank");
        }
        if (config.getTimeout().getModelCallMillis() <= 0) {
            throw new ConfigException("timeout.modelCallMillis must be greater than 0");
        }
    }
}
