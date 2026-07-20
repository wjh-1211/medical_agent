package com.medicalagent.knowledge;

import com.medicalagent.config.KnowledgeConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class KnowledgeService implements KnowledgeRetriever {

    private final KnowledgeConfig config;
    private final QueryEmbeddingModel embeddingModel;
    private final SqliteKnowledgeVectorStore vectorStore;
    private final KnowledgeDocumentLoader documentLoader;

    public KnowledgeService(
            KnowledgeConfig config,
            QueryEmbeddingModel embeddingModel,
            SqliteKnowledgeVectorStore vectorStore,
            KnowledgeDocumentLoader documentLoader
    ) {
        this.config = config;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.documentLoader = documentLoader;
    }

    public void rebuildIndex() {
        List<Document> documents = documentLoader.load(Path.of(config.getDocumentsDirectory()));
        DocumentSplitter splitter = DocumentSplitters.recursive(
                config.getChunkMaxCharacters(),
                config.getChunkOverlapCharacters()
        );
        List<TextSegment> segments = splitter.splitAll(documents);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        List<KnowledgeChunk> chunks = segments.stream().map(this::toChunk).toList();
        vectorStore.replaceAll(chunks, embeddings);
    }

    @Override
    public List<KnowledgeChunkMatch> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Knowledge search query must not be blank");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("Knowledge search topK must be greater than 0");
        }
        Embedding queryEmbedding = embeddingModel.embedQuery(query).content();
        return vectorStore.search(queryEmbedding, topK, config.getMinScore());
    }

    public int indexedChunkCount() {
        return vectorStore.count();
    }

    @Override
    public void close() {
        embeddingModel.close();
    }

    private KnowledgeChunk toChunk(TextSegment segment) {
        String content = segment.text().trim();
        String source = requiredMetadata(segment, "source");
        String section = requiredMetadata(segment, "section");
        String version = requiredMetadata(segment, "documentVersion");
        String contentHash = sha256(content);
        String chunkId = sha256(source + "\n" + section + "\n" + contentHash).substring(0, 24);
        return new KnowledgeChunk(chunkId, content, source, section, version, contentHash);
    }

    private String requiredMetadata(TextSegment segment, String field) {
        String value = segment.metadata().getString(field);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Knowledge segment is missing metadata field: " + field);
        }
        return value;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte current : digest) {
                result.append(String.format("%02x", current));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
