package com.medicalagent.memory;

import java.util.Optional;

public interface SummaryMemoryStore {

    Optional<SummaryMemorySnapshot> read(String sessionId);

    SummaryMemorySnapshot write(String sessionId, String summary);
}
