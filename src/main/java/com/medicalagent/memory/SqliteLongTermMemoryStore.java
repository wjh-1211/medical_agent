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
import java.util.ArrayList;
import java.util.List;

public class SqliteLongTermMemoryStore implements LongTermMemoryStore {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS long_term_memories (
                user_id TEXT NOT NULL,
                category TEXT NOT NULL,
                fact TEXT NOT NULL,
                source TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                UNIQUE(user_id, category, fact)
            )
            """;

    private final String jdbcUrl;
    private final Clock clock;

    public SqliteLongTermMemoryStore(Path databasePath) {
        this(databasePath, Clock.systemUTC());
    }

    SqliteLongTermMemoryStore(Path databasePath, Clock clock) {
        Path normalizedPath = databasePath.toAbsolutePath().normalize();
        createParentDirectory(normalizedPath);
        this.jdbcUrl = "jdbc:sqlite:" + normalizedPath;
        this.clock = clock;
        initialize();
    }

    @Override
    public List<LongTermMemoryRecord> read(String userId) {
        String normalizedUserId = requireText(userId, "userId");
        String sql = """
                SELECT user_id, category, fact, source, created_at, updated_at
                FROM long_term_memories
                WHERE user_id = ?
                ORDER BY updated_at DESC, category ASC, fact ASC
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<LongTermMemoryRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(mapRecord(resultSet));
                }
                return List.copyOf(records);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read long-term memory", exception);
        }
    }

    @Override
    public LongTermMemoryRecord upsert(String userId, LongTermMemoryCategory category, String fact, String source) {
        String normalizedUserId = requireText(userId, "userId");
        String normalizedFact = requireText(fact, "fact");
        String normalizedSource = requireText(source, "source");
        Instant now = clock.instant();
        String upsertSql = """
                INSERT INTO long_term_memories (user_id, category, fact, source, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id, category, fact) DO UPDATE SET
                    source = excluded.source,
                    updated_at = excluded.updated_at
                """;
        String selectSql = """
                SELECT user_id, category, fact, source, created_at, updated_at
                FROM long_term_memories
                WHERE user_id = ? AND category = ? AND fact = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement upsert = connection.prepareStatement(upsertSql);
             PreparedStatement select = connection.prepareStatement(selectSql)) {
            upsert.setString(1, normalizedUserId);
            upsert.setString(2, category.wireValue());
            upsert.setString(3, normalizedFact);
            upsert.setString(4, normalizedSource);
            upsert.setString(5, now.toString());
            upsert.setString(6, now.toString());
            upsert.executeUpdate();

            select.setString(1, normalizedUserId);
            select.setString(2, category.wireValue());
            select.setString(3, normalizedFact);
            try (ResultSet resultSet = select.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Long-term memory upsert did not return a record");
                }
                return mapRecord(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to write long-term memory", exception);
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
            throw new IllegalStateException("Failed to initialize long-term memory database", exception);
        }
    }

    private LongTermMemoryRecord mapRecord(ResultSet resultSet) throws SQLException {
        return new LongTermMemoryRecord(
                resultSet.getString("user_id"),
                LongTermMemoryCategory.fromWireValue(resultSet.getString("category")),
                resultSet.getString("fact"),
                resultSet.getString("source"),
                Instant.parse(resultSet.getString("created_at")),
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
            throw new IllegalStateException("Failed to create long-term memory directory", exception);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
