package com.medicalagent.model;

import com.medicalagent.config.ModelConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonTransformersLocalModelGatewayTest {

    @Test
    void shouldFailWhenLocalModelServerExitsDuringStartup() throws IOException {
        Path script = Files.createTempFile("medical-agent-timeout", ".py");
        Files.writeString(script, """
                import sys
                print("boom", file=sys.stderr)
                raise SystemExit(1)
                """, StandardCharsets.UTF_8);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setProvider("python_transformers");
        modelConfig.setName("failing-model");
        modelConfig.setPath("/tmp/dummy-model");
        modelConfig.setPythonExecutable("python3");
        modelConfig.setLauncherScript(script.toAbsolutePath().toString());

        PythonTransformersLocalModelGateway gateway = new PythonTransformersLocalModelGateway(modelConfig);

        LocalModelException exception = assertThrows(LocalModelException.class, gateway::preload);

        assertTrue(
                exception.getMessage().contains("startup")
                        || exception.getMessage().contains("not available")
                        || exception.getMessage().contains("Failed to start")
        );
    }

    @Test
    void shouldGenerateRequestsAfterPreloadUsingPersistentServer() throws IOException {
        Path script = Files.createTempFile("medical-agent-server", ".py");
        Files.writeString(script, """
                import json
                import sys

                if "--server" not in sys.argv:
                    print(json.dumps({"content": "single"}))
                    raise SystemExit(0)

                init_message = json.loads(sys.stdin.readline())
                if init_message.get("type") != "init":
                    raise SystemExit(1)

                print(json.dumps({"type": "status", "progress": 25, "message": "loading"}), flush=True)
                print(json.dumps({"type": "ready", "progress": 100, "message": "ready"}), flush=True)

                for line in sys.stdin:
                    if not line.strip():
                        continue
                    request = json.loads(line)
                    print(json.dumps({
                        "type": "response",
                        "requestId": request["requestId"],
                        "content": "echo:" + request["prompt"],
                        "modelName": request.get("modelName", ""),
                        "provider": "python_transformers"
                    }), flush=True)
                """, StandardCharsets.UTF_8);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setProvider("python_transformers");
        modelConfig.setName("persistent-model");
        modelConfig.setPath("/tmp/dummy-model");
        modelConfig.setPythonExecutable("python3");
        modelConfig.setLauncherScript(script.toAbsolutePath().toString());

        PythonTransformersLocalModelGateway gateway = new PythonTransformersLocalModelGateway(modelConfig);
        gateway.preload();

        LocalModelResponse first = gateway.generate(
                new LocalModelRequest("hello", "persistent-model", "/tmp/dummy-model", 0.0, 16, false, 500)
        );
        LocalModelResponse second = gateway.generate(
                new LocalModelRequest("world", "persistent-model", "/tmp/dummy-model", 0.0, 16, false, 500)
        );

        assertEquals("echo:hello", first.content());
        assertEquals("echo:world", second.content());

        gateway.shutdown();
    }
}
