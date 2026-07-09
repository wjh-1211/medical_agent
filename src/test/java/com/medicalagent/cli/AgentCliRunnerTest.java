package com.medicalagent.cli;

import com.medicalagent.api.AgentRequest;
import com.medicalagent.api.AgentResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCliRunnerTest {

    @Test
    void shouldRunChatLoopAppendHistoryAndHandleReset() {
        RecordingConsole console = new RecordingConsole(List.of(
                "我喉咙痛两天了",
                "昨晚开始加重",
                "/reset",
                "重新开始",
                "/exit"
        ));
        AgentCliSession session = new AgentCliSession("cli-user", new SessionIds("session-1", "session-2"));
        List<AgentRequest> requests = new ArrayList<>();

        AgentCliRunner runner = new AgentCliRunner(
                console,
                new CliCommandParser(),
                session,
                request -> {
                    requests.add(request);
                    LockSupport.parkNanos(120_000_000L);
                    return new AgentResponse(
                            "ok",
                            "req-" + requests.size(),
                            request.sessionId(),
                            request.userId(),
                            "reply-" + requests.size(),
                            false,
                            "2026-07-07T00:00:00Z"
                    );
                }
        );

        runner.run();

        assertEquals(3, requests.size());
        assertEquals("session-1", requests.get(0).sessionId());
        assertEquals(0, requests.get(0).history().size());
        assertEquals(2, requests.get(1).history().size());
        assertEquals("session-2", requests.get(2).sessionId());
        assertEquals(0, requests.get(2).history().size());
        assertTrue(console.outputs().stream().anyMatch(line -> line.contains("Agent> reply-1")));
        assertTrue(console.outputs().stream().anyMatch(line -> line.contains("思考中")));
        assertTrue(console.outputs().stream().anyMatch(line -> line.contains("Agent> Session reset.")));
        assertTrue(console.outputs().stream().anyMatch(line -> line.contains("Agent> Goodbye.")));
    }

    private static final class RecordingConsole implements CliConsole {

        private final Queue<String> inputs;
        private final List<String> outputs = new ArrayList<>();

        private RecordingConsole(List<String> inputs) {
            this.inputs = new ArrayDeque<>(inputs);
        }

        @Override
        public String readLine(String prompt) {
            outputs.add(prompt);
            return inputs.isEmpty() ? null : inputs.remove();
        }

        @Override
        public void print(String message) {
            outputs.add(message);
        }

        @Override
        public void println(String message) {
            outputs.add(message);
        }

        private List<String> outputs() {
            return outputs;
        }
    }

    private static final class SessionIds implements java.util.function.Supplier<String> {

        private final Queue<String> ids;

        private SessionIds(String... ids) {
            this.ids = new ArrayDeque<>(List.of(ids));
        }

        @Override
        public String get() {
            return ids.remove();
        }
    }
}
