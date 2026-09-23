package me.mykindos.betterpvp.core.world.settler.crew;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrewSpeedTest {

    private static final CrewLimits LIMITS = new CrewLimits(5, 4.0, Map.of(), 0.10);

    private static BuilderStats builder(double speed, double efficiency, String trade, String... compatible) {
        return new BuilderStats(2, speed, efficiency, trade, Set.of(compatible));
    }

    @Test
    void oneLegendaryRunsAtItsOwnSpeed() {
        assertEquals(2.2, CrewSpeed.of(List.of(builder(2.2, 0.9, "mason")), LIMITS), 1e-9);
    }

    @Test
    void theFastestCountsFullyAndTheRestAtTheirEfficiency() {
        final List<BuilderStats> laborers = List.of(builder(1, 0.5, "laborer"), builder(1, 0.5, "laborer"),
                builder(1, 0.5, "laborer"));
        assertEquals(2.0, CrewSpeed.of(laborers, LIMITS), 1e-9);
    }

    @Test
    void compatibleTradesCountFullyWithABonus() {
        final List<BuilderStats> crew = List.of(builder(1, 0.5, "mason", "carpenter"),
                builder(1, 0.5, "carpenter", "mason"));
        assertEquals(2.2, CrewSpeed.of(crew, LIMITS), 1e-9);
    }

    @Test
    void crewSpeedIsCapped() {
        final List<BuilderStats> crew = List.of(builder(2.2, 0.9, "mason"), builder(2.2, 0.9, "smith"),
                builder(2.2, 0.9, "laborer"));
        assertEquals(4.0, CrewSpeed.of(crew, LIMITS), 1e-9);
        assertEquals(0, CrewSpeed.of(List.of(), LIMITS));
        assertEquals(6, CrewSpeed.workforce(crew));
    }
}
