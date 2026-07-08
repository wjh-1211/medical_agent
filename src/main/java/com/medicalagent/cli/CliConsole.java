package com.medicalagent.cli;

public interface CliConsole {

    String readLine(String prompt);

    void println(String message);
}
