package com.medicalagent.cli;

import com.medicalagent.model.LocalModelLoadListener;

public class CliStartupLoadingListener implements LocalModelLoadListener {

    private static final int BAR_WIDTH = 20;

    private final CliConsole console;
    private int lastPercent = -1;
    private String lastMessage = "";
    private int lastRenderedLength = 0;

    public CliStartupLoadingListener(CliConsole console) {
        this.console = console;
    }

    @Override
    public void onProgress(int percent, String message) {
        int normalizedPercent = Math.max(0, Math.min(percent, 100));
        String normalizedMessage = message == null || message.isBlank()
                ? "模型加载中"
                : message.trim();
        if (normalizedPercent == lastPercent && normalizedMessage.equals(lastMessage)) {
            return;
        }
        lastPercent = normalizedPercent;
        lastMessage = normalizedMessage;

        int filled = (normalizedPercent * BAR_WIDTH) / 100;
        String bar = "#".repeat(filled) + "-".repeat(BAR_WIDTH - filled);
        String rendered = "Agent> [" + bar + "] " + normalizedPercent + "% " + normalizedMessage;
        int paddingWidth = Math.max(0, lastRenderedLength - rendered.length());
        String padded = rendered + " ".repeat(paddingWidth);
        lastRenderedLength = rendered.length();
        console.print("\r" + padded);
        if (normalizedPercent >= 100) {
            console.println("");
            lastRenderedLength = 0;
        }
    }
}
