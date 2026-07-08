package com.medicalagent.cli;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.api.AgentResponse;
import com.medicalagent.common.JsonSupport;

import java.util.Map;
import java.util.function.Function;

public class AgentCliRunner {

    private static final String HELP_TEXT = """
            Commands:
            /help  Show available commands
            /reset Start a new session
            /exit  Exit the CLI
            """;

    private final CliConsole console;
    private final CliCommandParser commandParser;
    private final AgentCliSession session;
    private final Function<AgentRequest, AgentResponse> requestHandler;

    public AgentCliRunner(
            CliConsole console,
            CliCommandParser commandParser,
            AgentCliSession session,
            Function<AgentRequest, AgentResponse> requestHandler
    ) {
        this.console = console;
        this.commandParser = commandParser;
        this.session = session;
        this.requestHandler = requestHandler;
    }

    public void run() {
        console.println("Medical Agent CLI");
        console.println("Type /help for commands.");

        while (true) {
            String rawInput = console.readLine("You> ");
            CliCommand command = commandParser.parse(rawInput);
            if (command.type() == CliCommand.Type.EMPTY) {
                continue;
            }
            if (command.type() == CliCommand.Type.HELP) {
                console.println(HELP_TEXT.stripTrailing());
                continue;
            }
            if (command.type() == CliCommand.Type.RESET) {
                session.reset();
                console.println("Agent> Session reset.");
                continue;
            }
            if (command.type() == CliCommand.Type.EXIT) {
                console.println("Agent> Goodbye.");
                return;
            }

            try {
                AgentResponse response = requestHandler.apply(buildRequest(command.payload()));
                console.println("Agent> " + response.answer());
                session.appendTurn(command.payload(), response.answer());
            } catch (RuntimeException exception) {
                console.println("Agent> Error: " + exception.getMessage());
            }
        }
    }

    private AgentRequest buildRequest(String message) {
        return new AgentRequest(
                message,
                session.sessionId(),
                session.userId(),
                session.history(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                null,
                Map.of("channel", "cli")
        );
    }
}
