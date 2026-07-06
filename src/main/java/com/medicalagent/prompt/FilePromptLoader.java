package com.medicalagent.prompt;

import com.medicalagent.config.PromptConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePromptLoader implements PromptLoader {

    private final PromptConfig promptConfig;

    public FilePromptLoader(PromptConfig promptConfig) {
        this.promptConfig = promptConfig;
    }

    @Override
    public PromptTemplate load(String templateName) {
        String resolvedName = (templateName == null || templateName.isBlank())
                ? promptConfig.getDefaultTemplate()
                : templateName.trim();
        Path promptPath = Path.of(promptConfig.getDirectory(), resolvedName + promptConfig.getFileExtension());
        if (!Files.exists(promptPath)) {
            throw new IllegalArgumentException("Prompt template not found: " + promptPath);
        }
        try {
            return new PromptTemplate(
                    resolvedName,
                    Files.readString(promptPath, StandardCharsets.UTF_8),
                    promptPath.toAbsolutePath()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load prompt template: " + promptPath, exception);
        }
    }
}
