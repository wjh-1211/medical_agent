package com.medicalagent.cli;

import java.util.concurrent.atomic.AtomicBoolean;

public class CliRequestTimer implements AutoCloseable {

    private static final long FRAME_INTERVAL_MILLIS = 100L;
    private static final char[] FRAMES = new char[]{'|', '/', '-', '\\'};

    private final CliConsole console;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final long startNanos = System.nanoTime();
    private Thread worker;
    private int lastRenderedLength = 0;

    public CliRequestTimer(CliConsole console) {
        this.console = console;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        render(0);
        worker = new Thread(this::runLoop, "cli-request-timer");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        Thread currentWorker = worker;
        if (currentWorker != null) {
            currentWorker.interrupt();
            try {
                currentWorker.join(300L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        clearLine();
        worker = null;
    }

    private void runLoop() {
        int frameIndex = 1;
        while (running.get()) {
            try {
                Thread.sleep(FRAME_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            render(frameIndex++);
        }
    }

    private void render(int frameIndex) {
        double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        char frame = FRAMES[frameIndex % FRAMES.length];
        String rendered = String.format("Agent> %c 思考中 %.1fs", frame, elapsedSeconds);
        int padding = Math.max(0, lastRenderedLength - rendered.length());
        console.print("\r" + rendered + " ".repeat(padding));
        lastRenderedLength = rendered.length();
    }

    private void clearLine() {
        if (lastRenderedLength == 0) {
            return;
        }
        console.print("\r" + " ".repeat(lastRenderedLength) + "\r");
        lastRenderedLength = 0;
    }
}
