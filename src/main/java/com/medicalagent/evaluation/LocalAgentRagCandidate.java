package com.medicalagent.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.agent.AgentDecisionParser;
import com.medicalagent.agent.AgentKernel;
import com.medicalagent.api.AgentRequest;
import com.medicalagent.common.JsonSupport;
import com.medicalagent.config.AppConfig;
import com.medicalagent.context.AgentContextFactory;
import com.medicalagent.knowledge.KnowledgeChunk;
import com.medicalagent.knowledge.KnowledgeChunkMatch;
import com.medicalagent.knowledge.KnowledgeService;
import com.medicalagent.prompt.FilePromptLoader;
import com.medicalagent.prompt.PromptRenderer;
import com.medicalagent.prompt.PromptVariablesFactory;
import com.medicalagent.runtime.ToolRouter;
import com.medicalagent.skills.SkillRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Optional non-deterministic sample candidate that uses the real local AgentKernel. */
public class LocalAgentRagCandidate implements RagEvaluationCandidate {

    private final AgentKernel agentKernel;
    private final AgentContextFactory contextFactory;
    private final RecordingKnowledgeSearchSkill knowledgeSearchSkill;
    private final RecordingLocalModelGateway modelGateway;

    public LocalAgentRagCandidate(
            AppConfig config,
            KnowledgeService knowledgeService,
            RecordingLocalModelGateway modelGateway
    ) {
        this.knowledgeSearchSkill = new RecordingKnowledgeSearchSkill(
                knowledgeService,
                config.getKnowledge().getDefaultTopK()
        );
        SkillRegistry skillRegistry = new SkillRegistry(config, List.of(knowledgeSearchSkill));
        this.agentKernel = new AgentKernel(
                config,
                skillRegistry,
                new ToolRouter(skillRegistry),
                new FilePromptLoader(config.getPrompt()),
                new PromptVariablesFactory(config.getContext()),
                new PromptRenderer(config.getPrompt()),
                modelGateway,
                new AgentDecisionParser()
        );
        this.contextFactory = new AgentContextFactory(config);
        this.modelGateway = modelGateway;
    }

    @Override
    public String candidateId() {
        return "vector-rag-local-agent";
    }

    @Override
    public String mode() {
        return "local-agent-sample";
    }

    @Override
    public boolean deterministic() {
        return false;
    }

    @Override
    public RagEvaluationExecution execute(RagEvaluationCase evaluationCase) {
        knowledgeSearchSkill.reset();
        modelGateway.reset();
        long startedAt = System.nanoTime();
        var response = agentKernel.handle(contextFactory.create(new AgentRequest(
                evaluationCase.query(),
                "evaluation-" + evaluationCase.caseId(),
                "evaluation-user",
                List.of(),
                null,
                JsonSupport.NODE_FACTORY.objectNode(),
                false,
                Map.of("channel", "evaluation")
        )));
        List<KnowledgeChunkMatch> matches = extractMatches(knowledgeSearchSkill.latestResult());
        return new RagEvaluationExecution(
                knowledgeSearchSkill.latestResult() != null,
                matches,
                response.answer(),
                0L,
                knowledgeSearchSkill.latestLatencyMillis(),
                modelGateway.decisionMillis(),
                modelGateway.mainModelMillis(),
                (System.nanoTime() - startedAt) / 1_000_000L
        );
    }

    private List<KnowledgeChunkMatch> extractMatches(JsonNode result) {
        if (result == null) {
            return List.of();
        }
        List<KnowledgeChunkMatch> matches = new ArrayList<>();
        for (JsonNode chunk : result.path("chunks")) {
            matches.add(new KnowledgeChunkMatch(
                    new KnowledgeChunk(
                            chunk.path("chunkId").asText(),
                            chunk.path("content").asText(),
                            chunk.path("source").asText(),
                            chunk.path("section").asText(),
                            chunk.path("documentVersion").asText(),
                            ""
                    ),
                    chunk.path("score").asDouble()
            ));
        }
        return matches;
    }
}
