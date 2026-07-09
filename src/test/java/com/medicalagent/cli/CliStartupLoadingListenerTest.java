package com.medicalagent.cli;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliStartupLoadingListenerTest {

    @Test
    void shouldPadShorterMessagesToClearPreviousContent() {
        RecordingConsole console = new RecordingConsole();
        CliStartupLoadingListener listener = new CliStartupLoadingListener(console);

        listener.onProgress(35, "正在加载模型权重 and some very long suffix");
        listener.onProgress(40, "短消息");

        String secondFrame = console.prints().get(1);
        assertTrue(secondFrame.startsWith("\rAgent> [########------------] 40% 短消息"));
        assertTrue(secondFrame.endsWith(" "));
    }

    @Test
    void shouldPrintTrailingNewlineWhenReady() {
        RecordingConsole console = new RecordingConsole();
        CliStartupLoadingListener listener = new CliStartupLoadingListener(console);

        listener.onProgress(100, "模型已就绪");

        assertEquals("", console.lines().get(0));
    }

    private static final class RecordingConsole implements CliConsole {

        private final List<String> prints = new ArrayList<>();
        private final List<String> lines = new ArrayList<>();

        @Override
        public String readLine(String prompt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void print(String message) {
            prints.add(message);
        }

        @Override
        public void println(String message) {
            lines.add(message);
        }

        private List<String> prints() {
            return prints;
        }

        private List<String> lines() {
            return lines;
        }
    }
}
