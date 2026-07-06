package com.medicalagent.prompt;

import com.medicalagent.config.PromptConfig;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    private final PromptConfig promptConfig;

    public PromptRenderer(PromptConfig promptConfig) {
        this.promptConfig = promptConfig;
    }

    public String render(PromptTemplate template, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template.content());
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.get(variableName);
            if (replacement == null) {
                if (promptConfig.isStrictVariables()) {
                    throw new IllegalArgumentException("Missing prompt variable: " + variableName);
                }
                replacement = "";
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
