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

    @Test
    void contributionsFollowTheCrewOrderAndAddUpToTheSpeed() {
        final List<BuilderStats> crew = List.of(builder(1, 0.5, "laborer"), builder(2, 0.5, "mason"));
        final double[] contributions = CrewSpeed.contributions(crew, LIMITS);
        assertEquals(0.5, contributions[0], 1e-9, "the slower one counts at its efficiency");
        assertEquals(2.0, contributions[1], 1e-9, "the fastest counts fully");
        assertEquals(CrewSpeed.of(crew, LIMITS), contributions[0] + contributions[1], 1e-9);
    }

    @Test
    void wastedSpeedIsWhatEfficiencyAndTheCapTakeAway() {
        final List<BuilderStats> crew = List.of(builder(1, 0.5, "laborer"), builder(2, 0.5, "mason"));
        final double[] wasted = CrewSpeed.wasted(crew, LIMITS);
        assertEquals(0.5, wasted[0], 1e-9);
        assertEquals(0.0, wasted[1], 1e-9);

        final List<BuilderStats> capped = List.of(builder(3, 1.0, "mason"), builder(3, 1.0, "smith"));
        final double[] over = CrewSpeed.wasted(capped, LIMITS);
        assertEquals(1.0, over[0], 1e-9, "6 against a cap of 4 loses a third of each");
        assertEquals(1.0, over[1], 1e-9);
    }

    @Test
    void aCompatibleBonusIsNeverWasted() {
        final List<BuilderStats> crew = List.of(builder(1, 0.5, "mason", "carpenter"),
                builder(1, 0.5, "carpenter", "mason"));
        final double[] wasted = CrewSpeed.wasted(crew, LIMITS);
        assertEquals(0.0, wasted[0], 1e-9);
        assertEquals(0.0, wasted[1], 1e-9);
    }
}
