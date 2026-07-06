package com.medicalagent.model;

public class LocalModelException extends RuntimeException {

    public LocalModelException(String message) {
        super(message);
    }

    public LocalModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
