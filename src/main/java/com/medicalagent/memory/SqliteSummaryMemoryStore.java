package com.medicalagent.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class SqliteSummaryMemoryStore implements SummaryMemoryStore {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS summary_memories (
                session_id TEXT PRIMARY KEY,
                summary TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """;

    private final String jdbcUrl;
    private final Clock clock;

    public SqliteSummaryMemoryStore(Path databasePath) {
        this(databasePath, Clock.systemUTC());
    }

    SqliteSummaryMemoryStore(Path databasePath, Clock clock) {
        Path normalizedPath = databasePath.toAbsolutePath().normalize();
        createParentDirectory(normalizedPath);
        this.jdbcUrl = "jdbc:sqlite:" + normalizedPath;
        this.clock = clock;
        initialize();
    }

    @Override
    public Optional<SummaryMemorySnapshot> read(String sessionId) {
        String normalizedSessionId = requireText(sessionId, "sessionId");
        String sql = "SELECT session_id, summary, updated_at FROM summary_memories WHERE session_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapSnapshot(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read summary memory", exception);
        }
    }

    @Override
    public SummaryMemorySnapshot write(String sessionId, String summary) {
        String normalizedSessionId = requireText(sessionId, "sessionId");
        String normalizedSummary = requireText(summary, "summary");
        Instant now = clock.instant();
        String sql = """
                INSERT INTO summary_memories (session_id, summary, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(session_id) DO UPDATE SET
                    summary = excluded.summary,
                    updated_at = excluded.updated_at
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedSessionId);
            statement.setString(2, normalizedSummary);
            statement.setString(3, now.toString());
            statement.executeUpdate();
            return new SummaryMemorySnapshot(normalizedSessionId, normalizedSummary, now);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to write summary memory", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initialize() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize summary memory database", exception);
        }
    }

    private SummaryMemorySnapshot mapSnapshot(ResultSet resultSet) throws SQLException {
        return new SummaryMemorySnapshot(
                resultSet.getString("session_id"),
                resultSet.getString("summary"),
                Instant.parse(resultSet.getString("updated_at"))
        );
    }

    private void createParentDirectory(Path databasePath) {
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create summary memory directory", exception);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
