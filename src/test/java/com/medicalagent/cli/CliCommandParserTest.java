package com.medicalagent.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliCommandParserTest {

    private final CliCommandParser parser = new CliCommandParser();

    @Test
    void shouldParseBuiltInCommands() {
        assertEquals(CliCommand.Type.HELP, parser.parse("/help").type());
        assertEquals(CliCommand.Type.RESET, parser.parse("/reset").type());
        assertEquals(CliCommand.Type.EXIT, parser.parse("/exit").type());
    }

    @Test
    void shouldTreatOtherInputAsMessage() {
        CliCommand command = parser.parse("我喉咙痛两天了");

        assertEquals(CliCommand.Type.MESSAGE, command.type());
        assertEquals("我喉咙痛两天了", command.payload());
    }
}
