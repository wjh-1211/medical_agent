package com.medicalagent.knowledge;

import com.medicalagent.common.JsonSupport;
import dev.langchain4j.data.embedding.Embedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** SQLite-backed vector index using LangChain4j Embedding values. */
public class SqliteKnowledgeVectorStore {

    private static final String INSERT_SQL = "INSERT INTO knowledge_chunks ("
            + "chunk_id, content, source, section_name, document_version, content_hash, embedding_json, created_at"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SQL = "SELECT chunk_id, content, source, section_name, document_version, "
            + "content_hash, embedding_json FROM knowledge_chunks";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS knowledge_chunks ("
            + "chunk_id TEXT PRIMARY KEY, "
            + "content TEXT NOT NULL, "
            + "source TEXT NOT NULL, "
            + "section_name TEXT NOT NULL, "
            + "document_version TEXT NOT NULL, "
            + "content_hash TEXT NOT NULL, "
            + "embedding_json TEXT NOT NULL, "
            + "created_at TEXT NOT NULL"
            + ")";

    private final Path databasePath;

    public SqliteKnowledgeVectorStore(Path databasePath) {
        this.databasePath = databasePath;
        initialize();
    }

    public synchronized void replaceAll(List<KnowledgeChunk> chunks, List<Embedding> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Knowledge chunks and embeddings must have the same size");
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM knowledge_chunks");
            }
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                for (int index = 0; index < chunks.size(); index++) {
                    KnowledgeChunk chunk = chunks.get(index);
                    statement.setString(1, chunk.chunkId());
                    statement.setString(2, chunk.content());
                    statement.setString(3, chunk.source());
                    statement.setString(4, chunk.section());
                    statement.setString(5, chunk.documentVersion());
                    statement.setString(6, chunk.contentHash());
                    statement.setString(7, JsonSupport.JSON_MAPPER.writeValueAsString(embeddings.get(index).vector()));
                    statement.setString(8, Instant.now().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Failed to replace knowledge vector index", exception);
        }
    }

    public List<KnowledgeChunkMatch> search(Embedding queryEmbedding, int topK, double minScore) {
        List<KnowledgeChunkMatch> matches = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                float[] storedEmbedding = JsonSupport.JSON_MAPPER.readValue(resultSet.getString("embedding_json"), float[].class);
                double score = cosine(queryEmbedding.vector(), storedEmbedding);
                if (score >= minScore) {
                    matches.add(new KnowledgeChunkMatch(
                            new KnowledgeChunk(
                                    resultSet.getString("chunk_id"),
                                    resultSet.getString("content"),
                                    resultSet.getString("source"),
                                    resultSet.getString("section_name"),
                                    resultSet.getString("document_version"),
                                    resultSet.getString("content_hash")
                            ),
                            score
                    ));
                }
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Failed to search knowledge vector index", exception);
        }
        return matches.stream()
                .sorted(Comparator.comparingDouble(KnowledgeChunkMatch::score).reversed())
                .limit(topK)
                .toList();
    }

    public int count() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM knowledge_chunks")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count knowledge chunks", exception);
        }
    }

    private void initialize() {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_TABLE_SQL);
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize knowledge vector index", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private double cosine(float[] left, float[] right) {
        if (left.length != right.length) {
            throw new IllegalStateException("Knowledge index embedding dimension does not match query embedding");
        }
        double dotProduct = 0d;
        double leftMagnitude = 0d;
        double rightMagnitude = 0d;
        for (int index = 0; index < left.length; index++) {
            dotProduct += left[index] * right[index];
            leftMagnitude += left[index] * left[index];
            rightMagnitude += right[index] * right[index];
        }
        if (leftMagnitude == 0d || rightMagnitude == 0d) {
            return 0d;
        }
        return dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }
}
