package com.medicalagent.prompt;

import java.nio.file.Path;

public record PromptTemplate(
        String name,
        String content,
        Path path
) {
}
