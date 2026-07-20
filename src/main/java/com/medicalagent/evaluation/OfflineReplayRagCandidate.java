package com.medicalagent.evaluation;

import com.medicalagent.knowledge.KnowledgeChunkMatch;
import com.medicalagent.knowledge.KnowledgeSearchTrace;
import com.medicalagent.knowledge.KnowledgeService;

import java.util.List;

/**
 * Deterministic offline replay. It exercises the real corpus, embedding model and vector retriever,
 * while replaying the expected Tool decision to keep regression metrics reproducible.
 */
public class OfflineReplayRagCandidate implements RagEvaluationCandidate {

    private final KnowledgeService knowledgeService;
    private final int topK;
    private final String candidateId;

    public OfflineReplayRagCandidate(KnowledgeService knowledgeService, int topK, String candidateId) {
        this.knowledgeService = knowledgeService;
        this.topK = topK;
        this.candidateId = candidateId;
    }

    @Override
    public String candidateId() {
        return candidateId;
    }

    @Override
    public String mode() {
        return "offline-replay";
    }

    @Override
    public boolean deterministic() {
        return true;
    }

    @Override
    public RagEvaluationExecution execute(RagEvaluationCase evaluationCase) {
        long startedAt = System.nanoTime();
        if (!evaluationCase.shouldRetrieve()) {
            return new RagEvaluationExecution(false, List.of(), "No controlled knowledge retrieval required.", 0L, 0L, 0L, 0L, elapsedMillis(startedAt));
        }
        KnowledgeSearchTrace trace = knowledgeService.searchWithTrace(evaluationCase.query(), topK);
        String answer = composeAnswer(trace.matches());
        return new RagEvaluationExecution(
                true,
                trace.matches(),
                answer,
                trace.embeddingMillis(),
                trace.retrievalMillis(),
                0L,
                0L,
                elapsedMillis(startedAt)
        );
    }

    private String composeAnswer(List<KnowledgeChunkMatch> matches) {
        if (matches.isEmpty()) {
            return "受控资料不足，当前没有可引用的证据。";
        }
        KnowledgeChunkMatch first = matches.get(0);
        return first.chunk().content()
                + " [source: " + first.chunk().source()
                + " | chunk: " + first.chunk().chunkId() + "]";
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
