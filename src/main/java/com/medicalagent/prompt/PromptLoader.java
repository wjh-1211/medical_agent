package com.medicalagent.prompt;

public interface PromptLoader {

    PromptTemplate load(String templateName);
}
