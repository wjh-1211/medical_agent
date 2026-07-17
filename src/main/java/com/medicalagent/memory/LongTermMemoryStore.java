package com.medicalagent.memory;

import java.util.List;

public interface LongTermMemoryStore {

    List<LongTermMemoryRecord> read(String userId);

    LongTermMemoryRecord upsert(String userId, LongTermMemoryCategory category, String fact, String source);
}
