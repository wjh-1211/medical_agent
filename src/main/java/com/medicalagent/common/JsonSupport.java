package com.medicalagent.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class JsonSupport {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    private JsonSupport() {
    }
}
