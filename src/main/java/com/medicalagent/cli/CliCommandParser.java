package com.medicalagent.cli;

public class CliCommandParser {

    public CliCommand parse(String rawInput) {
        if (rawInput == null) {
            return CliCommand.of(CliCommand.Type.EXIT);
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return CliCommand.of(CliCommand.Type.EMPTY);
        }
        return switch (trimmed) {
            case "/help" -> CliCommand.of(CliCommand.Type.HELP);
            case "/reset" -> CliCommand.of(CliCommand.Type.RESET);
            case "/exit" -> CliCommand.of(CliCommand.Type.EXIT);
            default -> CliCommand.message(rawInput);
        };
    }
}
