package com.medicalagent.config;

public class SessionConfig {

    private boolean autoCreateSessionId = true;
    private boolean allowAnonymousUser = true;
    private String anonymousUserIdPrefix = "anonymous";

    public boolean isAutoCreateSessionId() {
        return autoCreateSessionId;
    }

    public void setAutoCreateSessionId(boolean autoCreateSessionId) {
        this.autoCreateSessionId = autoCreateSessionId;
    }

    public boolean isAllowAnonymousUser() {
        return allowAnonymousUser;
    }

    public void setAllowAnonymousUser(boolean allowAnonymousUser) {
        this.allowAnonymousUser = allowAnonymousUser;
    }

    public String getAnonymousUserIdPrefix() {
        return anonymousUserIdPrefix;
    }

    public void setAnonymousUserIdPrefix(String anonymousUserIdPrefix) {
        this.anonymousUserIdPrefix = anonymousUserIdPrefix;
    }
}
