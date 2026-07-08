package com.medicalagent.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class SystemCliConsole implements CliConsole {

    private final BufferedReader reader;
    private final PrintStream out;

    public SystemCliConsole() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out);
    }

    SystemCliConsole(BufferedReader reader, PrintStream out) {
        this.reader = reader;
        this.out = out;
    }

    @Override
    public String readLine(String prompt) {
        try {
            out.print(prompt);
            out.flush();
            return reader.readLine();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read CLI input", exception);
        }
    }

    @Override
    public void println(String message) {
        out.println(message);
        out.flush();
    }
}
