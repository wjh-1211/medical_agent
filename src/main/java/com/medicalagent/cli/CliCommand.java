package com.medicalagent.cli;

public record CliCommand(
        Type type,
        String payload
) {

    public static CliCommand message(String payload) {
        return new CliCommand(Type.MESSAGE, payload);
    }

    public static CliCommand of(Type type) {
        return new CliCommand(type, null);
    }

    public enum Type {
        MESSAGE,
        HELP,
        RESET,
        EXIT,
        EMPTY
    }
}
