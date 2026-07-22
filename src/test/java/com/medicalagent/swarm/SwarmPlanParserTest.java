package com.medicalagent.swarm;

import com.medicalagent.config.SwarmConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SwarmPlanParserTest {

    private final SwarmPlanParser parser = new SwarmPlanParser();
    private final SwarmConfig config = new SwarmConfig();

    @Test
    void shouldParseSingleAgentPlan() {
        SwarmPlan plan = parser.parse("{\"mode\":\"single_agent\",\"tasks\":[]}", config);

        assertEquals(SwarmPlanMode.SINGLE_AGENT, plan.mode());
        assertEquals(0, plan.tasks().size());
    }

    @Test
    void shouldParseBoundedComplexPlan() {
        SwarmPlan plan = parser.parse("""
                {"mode":"swarm","tasks":[
                  {"role":"memory","query":""},
                  {"role":"retriever","query":"呼吸困难 就医提示"},
                  {"role":"safety","query":""}
                ]}
                """, config);

        assertEquals(SwarmPlanMode.SWARM, plan.mode());
        assertEquals(3, plan.tasks().size());
        assertEquals(SwarmRole.RETRIEVER, plan.tasks().get(1).role());
    }

    @Test
    void shouldRejectInvalidOrUnsafePlans() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{\"mode\":\"parallel\",\"tasks\":[]}", config));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "{\"mode\":\"swarm\",\"tasks\":[{\"role\":\"retriever\",\"query\":\"\"},{\"role\":\"safety\",\"query\":\"\"}]}", config));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "{\"mode\":\"swarm\",\"tasks\":[{\"role\":\"safety\",\"query\":\"\"},{\"role\":\"safety\",\"query\":\"\"}]}", config));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "{\"mode\":\"swarm\",\"tasks\":[{\"role\":\"memory\",\"query\":\"\"},{\"role\":\"retriever\",\"query\":\"q\"},{\"role\":\"safety\",\"query\":\"\"},{\"role\":\"answer\",\"query\":\"\"},{\"role\":\"memory\",\"query\":\"\"}]}", config));
    }
}
