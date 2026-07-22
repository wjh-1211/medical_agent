package com.medicalagent.evaluation;

import com.medicalagent.common.JsonSupport;
import com.medicalagent.memory.InMemorySessionMemoryStore;
import com.medicalagent.skills.MemoryReadSkill;
import com.medicalagent.skills.MemoryWriteSkill;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Deterministic evaluator that exercises the real Session Memory Skill pair. */
public class MemoryRecallEvaluator {

    public MemoryRecallEvaluation evaluate() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(Duration.ofMinutes(30));
        MemoryWriteSkill writer = new MemoryWriteSkill(store);
        MemoryReadSkill reader = new MemoryReadSkill(store);
        List<MemoryCase> cases = List.of(
                new MemoryCase("memory-allergy", "我对青霉素过敏", "已记录过敏史"),
                new MemoryCase("memory-medication", "我长期服用降压药", "已记录长期用药"),
                new MemoryCase("memory-symptom", "我今天开始咳嗽", "已记录当前症状")
        );
        List<String> failures = new ArrayList<>();
        for (MemoryCase evaluationCase : cases) {
            writer.execute(JsonSupport.NODE_FACTORY.objectNode()
                    .put("sessionId", evaluationCase.sessionId())
                    .put("userMessage", evaluationCase.userMessage())
                    .put("agentAnswer", evaluationCase.agentAnswer()));
            var recalled = reader.execute(JsonSupport.NODE_FACTORY.objectNode().put("sessionId", evaluationCase.sessionId()));
            if (!recalled.path("found").asBoolean()
                    || !evaluationCase.userMessage().equals(recalled.path("lastUserMessage").asText())
                    || !evaluationCase.agentAnswer().equals(recalled.path("lastAgentAnswer").asText())) {
                failures.add(evaluationCase.sessionId());
            }
        }
        int passed = cases.size() - failures.size();
        return new MemoryRecallEvaluation(cases.size(), passed, passed / (double) cases.size(), failures);
    }

    private record MemoryCase(String sessionId, String userMessage, String agentAnswer) {
    }
}
