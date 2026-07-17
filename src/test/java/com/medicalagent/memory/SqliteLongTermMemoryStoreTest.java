package com.medicalagent.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteLongTermMemoryStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldPersistAndDeduplicateUserFactsAcrossStoreInstances() {
        Path databasePath = temporaryDirectory.resolve("long-term-memory.db");
        SqliteLongTermMemoryStore firstStore = new SqliteLongTermMemoryStore(databasePath);

        firstStore.upsert(
                "user-1",
                LongTermMemoryCategory.ALLERGY,
                "Allergic to penicillin",
                "user_confirmed"
        );
        firstStore.upsert(
                "user-1",
                LongTermMemoryCategory.ALLERGY,
                "Allergic to penicillin",
                "user_confirmed"
        );

        SqliteLongTermMemoryStore secondStore = new SqliteLongTermMemoryStore(databasePath);
        var records = secondStore.read("user-1");

        assertEquals(1, records.size());
        assertEquals(LongTermMemoryCategory.ALLERGY, records.get(0).category());
        assertEquals("Allergic to penicillin", records.get(0).fact());
        assertTrue(records.get(0).createdAt().isBefore(records.get(0).updatedAt())
                || records.get(0).createdAt().equals(records.get(0).updatedAt()));
    }
}
