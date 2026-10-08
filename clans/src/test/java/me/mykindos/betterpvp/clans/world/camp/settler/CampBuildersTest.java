package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.settler.crew.CrewLimits;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a camp's Builders bring, with the shipped settlers.yml. */
class CampBuildersTest {

    private static final Job PLAIN = job(JobKind.BUILD, Map.of(), Duration.ofMinutes(1));

    private final SettlerConfig config = ShippedSettlers.config();
    private final CampBuilders builders = new CampBuilders(config);

    private static Settler builder(SettlerRarity rarity, String trade, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(rarity);
        settler.setProfession(CampProfessions.BUILDER);
        settler.setSpecialty(trade);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        return settler;
    }

    private static Settler builder(String... traits) {
        return builder(SettlerRarity.COMMON, null, traits);
    }

    private static Job job(JobKind kind, Map<String, Integer> cost, Duration duration) {
        return Job.start(kind, duration, ResourceCost.of(cost), 0, 0);
    }

    private BuilderStats alone(Settler settler, Job job) {
        return builders.stats(settler, job, List.of(settler));
    }

    private double speed(Settler settler, Job job) {
        return alone(settler, job).getSpeed();
    }

    @Test
    void ac5_eachRarityBringsItsWorkforceSpeedAndEfficiency() {
        final Map<SettlerRarity, BuilderStats> prd = Map.of(
                SettlerRarity.COMMON, new BuilderStats(2, 1.0, 0.50, null, Set.of()),
                SettlerRarity.UNCOMMON, new BuilderStats(3, 1.25, 0.60, null, Set.of()),
                SettlerRarity.RARE, new BuilderStats(4, 1.6, 0.75, null, Set.of()),
                SettlerRarity.LEGENDARY, new BuilderStats(6, 2.2, 0.90, null, Set.of()));
        prd.forEach((rarity, expected) -> {
            final BuilderStats stats = alone(builder(rarity, null), PLAIN);
            assertEquals(expected.getWorkforce(), stats.getWorkforce(), rarity.name());
            assertEquals(expected.getSpeed(), stats.getSpeed(), 1e-9, rarity.name());
            assertEquals(expected.getEfficiency(), stats.getEfficiency(), 1e-9, rarity.name());
        });
    }

    @Test
    void ac6_aTradeSpeedsUpTheJobsMostlyOfItsResource() {
        final Job stone = job(JobKind.BUILD, Map.of("stone", 10, "wood", 5), Duration.ofMinutes(10));
        final Job wood = job(JobKind.BUILD, Map.of("stone", 5, "wood", 10), Duration.ofMinutes(10));
        final Job iron = job(JobKind.REPAIR, Map.of("iron", 10, "wood", 3), Duration.ofMinutes(10));

        assertEquals(1.5, speed(builder(SettlerRarity.COMMON, CampProfessions.MASON), stone), 1e-9);
        assertEquals(1.0, speed(builder(SettlerRarity.COMMON, CampProfessions.MASON), wood), 1e-9);
        assertEquals(1.5, speed(builder(SettlerRarity.COMMON, CampProfessions.CARPENTER), wood), 1e-9);
        assertEquals(1.0, speed(builder(SettlerRarity.COMMON, CampProfessions.CARPENTER), stone), 1e-9);
        assertEquals(1.5, speed(builder(SettlerRarity.COMMON, CampProfessions.SMITH), iron), 1e-9);
        assertEquals(2.2 * 1.5, speed(builder(SettlerRarity.LEGENDARY, CampProfessions.MASON), stone), 1e-9);
    }

    @Test
    void ac6_aTieIsNoOneResource() {
        assertFalse(CampBuilders.mostly(job(JobKind.BUILD, Map.of("wood", 5, "stone", 5), Duration.ZERO), "wood"));
        assertTrue(CampBuilders.mostly(job(JobKind.BUILD, Map.of("wood", 6, "stone", 5), Duration.ZERO), "wood"));
        assertFalse(CampBuilders.mostly(job(JobKind.BUILD, Map.of(), Duration.ZERO), "wood"));
    }

    @Test
    void ac6_aLaborerBringsExtraWorkforceAndNoSpecialty() {
        final Settler laborer = builder(SettlerRarity.COMMON, CampProfessions.LABORER);
        for (String resource : List.of("stone", "wood", "iron")) {
            final BuilderStats stats = alone(laborer, job(JobKind.BUILD, Map.of(resource, 10), Duration.ofMinutes(1)));
            assertEquals(3, stats.getWorkforce());
            assertEquals(1.0, stats.getSpeed(), 1e-9);
        }
    }

    @Test
    void ac7_compatibleTradesGoBothWays() {
        final Map<String, Set<String>> compatible = Map.of(
                CampProfessions.MASON, Set.of(CampProfessions.CARPENTER, CampProfessions.SMITH),
                CampProfessions.CARPENTER, Set.of(CampProfessions.MASON, CampProfessions.LABORER),
                CampProfessions.SMITH, Set.of(CampProfessions.MASON),
                CampProfessions.LABORER, Set.of(CampProfessions.CARPENTER));
        compatible.forEach((trade, with) -> {
            final BuilderStats stats = alone(builder(SettlerRarity.COMMON, trade), PLAIN);
            assertEquals(trade, stats.getTrade());
            assertEquals(with, stats.getCompatible(), trade);
        });
        assertTrue(alone(builder(), PLAIN).getCompatible().isEmpty());
        assertNull(alone(builder(), PLAIN).getTrade());
    }

    @Test
    void ac7_theShippedCrewLimitsAreThePrds() {
        final CrewLimits limits = config.getCrewLimits();
        assertEquals(5, limits.getMaxSize());
        assertEquals(4.0, limits.getMaxSpeed(), 1e-9);
        assertEquals(Map.of(SettlerRarity.RARE, 2, SettlerRarity.LEGENDARY, 1), limits.getPerRarity());
        assertEquals(0.10, limits.getCompatibleBonus(), 1e-9);
    }

    @Test
    void ac8_traitsGrowWithRarityButTradeOffCostsDoNot() {
        assertEquals(3 + 1, alone(builder(SettlerRarity.UNCOMMON, null, CampTraits.STEADY_HANDS), PLAIN).getWorkforce());
        assertEquals(4 + 2, alone(builder(SettlerRarity.RARE, null, CampTraits.STEADY_HANDS), PLAIN).getWorkforce());
        assertEquals(6 + 2, alone(builder(SettlerRarity.LEGENDARY, null, CampTraits.STEADY_HANDS), PLAIN).getWorkforce());

        final BuilderStats brawny = alone(builder(SettlerRarity.LEGENDARY, null, CampTraits.BRAWNY), PLAIN);
        assertEquals(6 + 4, brawny.getWorkforce());
        assertEquals(2.2 * 0.9, brawny.getSpeed(), 1e-9, "the speed cost is not doubled");

        final Settler loner = builder(SettlerRarity.LEGENDARY, null, CampTraits.LONER);
        assertEquals(2.2 * 1.8, speed(loner, PLAIN), 1e-9);
        assertEquals(2.2 * 0.8, builders.stats(loner, PLAIN, List.of(loner, builder())).getSpeed(), 1e-9);
    }

    @Test
    void ac9_steadyHandsAddsOneWorkforce() {
        assertEquals(3, alone(builder(CampTraits.STEADY_HANDS), PLAIN).getWorkforce());
        assertEquals(1.0, speed(builder(CampTraits.STEADY_HANDS), PLAIN), 1e-9);
    }

    @Test
    void ac10_tirelessOnlySpeedsUpJobsLongerThanTwoHours() {
        final Settler tireless = builder(CampTraits.TIRELESS);
        assertEquals(1.15, speed(tireless, job(JobKind.BUILD, Map.of(), Duration.ofHours(3))), 1e-9);
        assertEquals(1.0, speed(tireless, job(JobKind.BUILD, Map.of(), Duration.ofHours(2))), 1e-9);
        assertEquals(1.0, speed(tireless, job(JobKind.BUILD, Map.of(), Duration.ofMinutes(30))), 1e-9);
        assertEquals(2.2 * 1.3, speed(builder(SettlerRarity.LEGENDARY, null, CampTraits.TIRELESS),
                job(JobKind.BUILD, Map.of(), Duration.ofHours(3))), 1e-9);
    }

    @Test
    void ac11_onlyTheBestFrugalHandsBackPartOfTheCost() {
        final Job build = job(JobKind.BUILD, Map.of(), Duration.ZERO);
        assertEquals(0.05, builders.refund(build, List.of(builder(CampTraits.FRUGAL))), 1e-9);
        assertEquals(0.10, builders.refund(build, List.of(builder(CampTraits.FRUGAL),
                builder(SettlerRarity.LEGENDARY, null, CampTraits.FRUGAL))), 1e-9);
        assertEquals(0, builders.refund(build, List.of(builder())), 1e-9);
    }

    @Test
    void ac12_patcherSpeedsUpAndRefundsOnlyRepairs() {
        final Settler patcher = builder(CampTraits.PATCHER);
        final Job repair = job(JobKind.REPAIR, Map.of(), Duration.ofMinutes(5));
        final Job build = job(JobKind.BUILD, Map.of(), Duration.ofMinutes(5));

        assertEquals(1.2, speed(patcher, repair), 1e-9);
        assertEquals(1.0, speed(patcher, build), 1e-9);
        assertEquals(0.20, builders.refund(repair, List.of(patcher)), 1e-9);
        assertEquals(0, builders.refund(build, List.of(patcher)), 1e-9);
    }

    @Test
    void ac12_frugalAndPatcherRefundsAddUpToTheWholeCostAtMost() {
        final Settler frugal = builder(CampTraits.FRUGAL);
        final Settler patcher = builder(SettlerRarity.LEGENDARY, null, CampTraits.PATCHER);
        assertEquals(0.05 + 0.40, builders.refund(job(JobKind.REPAIR, Map.of(), Duration.ZERO),
                List.of(frugal, patcher)), 1e-9);

        final CampBuilders generous = new CampBuilders(ShippedSettlers.config(yaml -> {
            yaml.set("traits.frugal.refund", 0.5);
            yaml.set("traits.patcher.refund", 0.8);
        }));
        assertEquals(1.0, generous.refund(job(JobKind.REPAIR, Map.of(), Duration.ZERO),
                List.of(builder(CampTraits.FRUGAL), builder(CampTraits.PATCHER))), 1e-9);
    }

    @Test
    void ac13_oneForemanSpeedsUpEveryoneElse() {
        final Settler foreman = builder(CampTraits.FOREMAN);
        final Settler other = builder();
        final List<Settler> crew = List.of(foreman, other);

        assertEquals(1.1, builders.stats(other, PLAIN, crew).getSpeed(), 1e-9);
        assertEquals(1.0, builders.stats(foreman, PLAIN, crew).getSpeed(), 1e-9, "not itself");
    }

    @Test
    void ac13_onlyTheFirstOfEquallyStrongForemenCounts() {
        final Settler first = builder(CampTraits.FOREMAN);
        final Settler other = builder();
        final Settler second = builder(CampTraits.FOREMAN);
        final List<Settler> crew = List.of(first, other, second);

        assertEquals(1.1, builders.stats(other, PLAIN, crew).getSpeed(), 1e-9);
        assertEquals(1.1, builders.stats(second, PLAIN, crew).getSpeed(), 1e-9, "a second Foreman is sped up");
        assertEquals(1.0, builders.stats(first, PLAIN, crew).getSpeed(), 1e-9,
                "the Foreman that counts gets nothing from a second one");
    }

    @Test
    void ac13_theStrongestForemanCounts() {
        final Settler common = builder(CampTraits.FOREMAN);
        final Settler legendary = builder(SettlerRarity.LEGENDARY, null, CampTraits.FOREMAN);
        final Settler other = builder();
        final List<Settler> crew = List.of(common, legendary, other);

        assertEquals(1.2, builders.stats(other, PLAIN, crew).getSpeed(), 1e-9);
        assertEquals(1.2, builders.stats(common, PLAIN, crew).getSpeed(), 1e-9);
        assertEquals(2.2, builders.stats(legendary, PLAIN, crew).getSpeed(), 1e-9);
    }

    @Test
    void ac14_quickStudyGrowsWithJobsFinishedUpToItsCap() {
        final Settler student = builder(CampTraits.QUICK_STUDY);
        final Map<Integer, Double> speeds = Map.of(0, 1.0, 4, 1.0, 5, 1.05, 12, 1.10, 25, 1.25, 100, 1.25);
        speeds.forEach((jobs, expected) -> {
            student.setJobsFinished(jobs);
            assertEquals(expected, speed(student, PLAIN), 1e-9, jobs + " jobs");
        });

        final Settler legendary = builder(SettlerRarity.LEGENDARY, null, CampTraits.QUICK_STUDY);
        legendary.setJobsFinished(100);
        assertEquals(2.2 * 1.5, speed(legendary, PLAIN), 1e-9);
    }

    @Test
    void ac15_architectsEyeOnlySpeedsUpStageAdvances() {
        final Settler architect = builder(CampTraits.ARCHITECTS_EYE);
        assertEquals(1.15, speed(architect, job(JobKind.ADVANCE, Map.of(), Duration.ofMinutes(1))), 1e-9);
        assertEquals(1.0, speed(architect, job(JobKind.BUILD, Map.of(), Duration.ofMinutes(1))), 1e-9);
        assertEquals(1.0, speed(architect, job(JobKind.REPAIR, Map.of(), Duration.ofMinutes(1))), 1e-9);
    }

    @Test
    void ac16_lonersPreferToWorkAlone() {
        final Settler loner = builder(CampTraits.LONER);
        assertEquals(1.4, speed(loner, PLAIN), 1e-9);
        assertEquals(0.8, builders.stats(loner, PLAIN, List.of(loner, builder())).getSpeed(), 1e-9);
    }

    @Test
    void ac17_greedyIsFaster() {
        assertEquals(1.15, speed(builder(CampTraits.GREEDY), PLAIN), 1e-9);
        assertEquals(2.2 * 1.3, speed(builder(SettlerRarity.LEGENDARY, null, CampTraits.GREEDY), PLAIN), 1e-9);
    }

    @Test
    void ac18_brawnyIsStrongerAndSlower() {
        final BuilderStats stats = alone(builder(CampTraits.BRAWNY), PLAIN);
        assertEquals(4, stats.getWorkforce());
        assertEquals(0.9, stats.getSpeed(), 1e-9);
    }

    @Test
    void ac19_prodigyAndHomesickRaiseWorkforceAndSpeedAtStrengthAndACommonHomesickKeepsTwoWorkforce() {
        final BuilderStats prodigy = alone(builder(SettlerRarity.LEGENDARY, null, CampTraits.PRODIGY), PLAIN);
        assertEquals(9, prodigy.getWorkforce(), "6 raised by 25% at x2");
        assertEquals(3.3, prodigy.getSpeed(), 1e-9, "2.2 raised by 25% at x2");

        final BuilderStats homesick = alone(builder(CampTraits.HOMESICK), PLAIN);
        assertEquals(2, homesick.getWorkforce(), "2.2 rounds back down to 2");
        assertEquals(1.1, homesick.getSpeed(), 1e-9);

        final BuilderStats rare = alone(builder(SettlerRarity.RARE, null, CampTraits.HOMESICK), PLAIN);
        assertEquals(5, rare.getWorkforce(), "4 raised by 10% at x1.5 is 4.6, rounded");
        assertEquals(1.84, rare.getSpeed(), 1e-9, "1.6 raised by 10% at x1.5");
    }
}
