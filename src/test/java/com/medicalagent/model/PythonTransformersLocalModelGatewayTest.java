package com.medicalagent.model;

import com.medicalagent.config.ModelConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonTransformersLocalModelGatewayTest {

    @Test
    void shouldPropagateTimeoutWhenLocalModelProcessRunsTooLong() throws IOException {
        Path script = Files.createTempFile("medical-agent-timeout", ".py");
        Files.writeString(script, """
                import time
                time.sleep(2)
                print('{"content":"late"}')
                """, StandardCharsets.UTF_8);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setProvider("python_transformers");
        modelConfig.setName("timeout-model");
        modelConfig.setPath("/tmp/dummy-model");
        modelConfig.setPythonExecutable("python3");
        modelConfig.setLauncherScript(script.toAbsolutePath().toString());

        PythonTransformersLocalModelGateway gateway = new PythonTransformersLocalModelGateway(modelConfig);

        LocalModelException exception = assertThrows(LocalModelException.class, () -> gateway.generate(
                new LocalModelRequest("prompt", "timeout-model", "/tmp/dummy-model", 0.0, 16, false, 200)
        ));

        assertTrue(exception.getMessage().contains("timed out"));
    }
}
