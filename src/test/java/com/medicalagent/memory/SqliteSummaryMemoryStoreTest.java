package com.medicalagent.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteSummaryMemoryStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldPersistLatestSummaryAcrossStoreInstances() {
        Path databasePath = temporaryDirectory.resolve("summary-memory.db");
        SqliteSummaryMemoryStore firstStore = new SqliteSummaryMemoryStore(databasePath);
        firstStore.write("session-1", "Initial symptoms: cough for two days.");
        firstStore.write("session-1", "Symptoms: cough for two days; fever reached 38.5C.");

        SqliteSummaryMemoryStore secondStore = new SqliteSummaryMemoryStore(databasePath);
        var snapshot = secondStore.read("session-1");

        assertTrue(snapshot.isPresent());
        assertEquals("Symptoms: cough for two days; fever reached 38.5C.", snapshot.get().summary());
    }
}
