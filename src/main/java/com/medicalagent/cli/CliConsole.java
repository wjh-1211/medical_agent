package com.medicalagent.cli;

public interface CliConsole {

    String readLine(String prompt);

    void print(String message);

    void println(String message);
}
