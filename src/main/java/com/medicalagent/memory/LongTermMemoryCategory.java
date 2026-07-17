package com.medicalagent.memory;

import java.util.Arrays;

public enum LongTermMemoryCategory {

    MEDICAL_HISTORY("medical_history"),
    ALLERGY("allergy"),
    LONG_TERM_MEDICATION("long_term_medication");

    private final String wireValue;

    LongTermMemoryCategory(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static LongTermMemoryCategory fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(category -> category.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported long-term memory category: " + value));
    }
}
