package com.medicalagent.config;

public class ConfigProvider {

    private final ConfigLoader configLoader;
    private final String profile;

    public ConfigProvider(ConfigLoader configLoader, String profile) {
        this.configLoader = configLoader;
        this.profile = profile;
    }

    public AppConfig get() {
        return configLoader.load(profile);
    }
}
