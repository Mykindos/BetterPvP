package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampBuildersTest {

    private final SettlerConfig config = mock(SettlerConfig.class);
    private final CampBuilders builders = new CampBuilders(config);

    @BeforeEach
    void setUp() {
        when(config.builder(SettlerRarity.COMMON)).thenReturn(new SettlerConfig.BuilderNumbers(2, 1.0, 0.5));
        when(config.builder(SettlerRarity.LEGENDARY)).thenReturn(new SettlerConfig.BuilderNumbers(6, 2.2, 0.9));
        when(config.getTable()).thenReturn(new SettlerTable(Map.of(
                SettlerRarity.COMMON, new RarityNumbers(1, 1, 1, 0.3),
                SettlerRarity.LEGENDARY, new RarityNumbers(3, 2, 2.2, 0.05)), List.of(), List.of(), Map.of()));
        when(config.trade(null)).thenReturn(Optional.empty());
        when(config.trade("mason")).thenReturn(Optional.of(new SettlerConfig.Trade("stone", 0.5, 0, Set.of("carpenter"))));
        when(config.trade("laborer")).thenReturn(Optional.of(new SettlerConfig.Trade(null, 0, 1, Set.of("carpenter"))));
        when(config.trait(anyString(), anyString(), anyDouble())).thenAnswer(invocation -> invocation.getArgument(2));
    }

    private static Settler builder(SettlerRarity rarity, String trade, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(rarity);
        settler.setProfession(CampProfessions.BUILDER);
        settler.setSpecialty(trade);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        return settler;
    }

    private static Job job(JobKind kind, Map<String, Integer> cost, Duration duration) {
        return Job.start(kind, duration, ResourceCost.of(cost), 0, 0);
    }

    @Test
    void aTradeSpeedsUpTheJobsItSuits() {
        final Settler mason = builder(SettlerRarity.COMMON, "mason");
        final BuilderStats onStone = builders.stats(mason, job(JobKind.BUILD, Map.of("stone", 10, "wood", 5),
                Duration.ofMinutes(10)), List.of(mason));
        final BuilderStats onWood = builders.stats(mason, job(JobKind.BUILD, Map.of("stone", 5, "wood", 10),
                Duration.ofMinutes(10)), List.of(mason));

        assertEquals(1.5, onStone.getSpeed(), 1e-9);
        assertEquals(1.0, onWood.getSpeed(), 1e-9);
        assertEquals(Set.of("carpenter"), onStone.getCompatible());
    }

    @Test
    void aLaborerBringsExtraWorkforce() {
        final Settler laborer = builder(SettlerRarity.COMMON, "laborer");
        assertEquals(3, builders.stats(laborer, job(JobKind.BUILD, Map.of(), Duration.ofMinutes(1)), List.of(laborer))
                .getWorkforce());
    }

    @Test
    void traitsGrowWithRarityButTradeOffCostsDoNot() {
        final Settler common = builder(SettlerRarity.COMMON, null, CampTraits.STEADY_HANDS, CampTraits.BRAWNY);
        final Settler legendary = builder(SettlerRarity.LEGENDARY, null, CampTraits.STEADY_HANDS, CampTraits.BRAWNY);
        final Job job = job(JobKind.BUILD, Map.of(), Duration.ofMinutes(1));

        final BuilderStats low = builders.stats(common, job, List.of(common));
        final BuilderStats high = builders.stats(legendary, job, List.of(legendary));
        assertEquals(2 + 1 + 2, low.getWorkforce());
        assertEquals(0.9, low.getSpeed(), 1e-9);
        assertEquals(6 + 2 + 4, high.getWorkforce());
        assertEquals(2.2 * 0.9, high.getSpeed(), 1e-9);
    }

    @Test
    void oneForemanSpeedsUpEveryoneElse() {
        final Settler foreman = builder(SettlerRarity.COMMON, null, CampTraits.FOREMAN);
        final Settler other = builder(SettlerRarity.COMMON, null);
        final Settler second = builder(SettlerRarity.COMMON, null, CampTraits.FOREMAN);
        final Job job = job(JobKind.BUILD, Map.of(), Duration.ofMinutes(1));
        final List<Settler> crew = List.of(foreman, other, second);

        assertEquals(1.1, builders.stats(other, job, crew).getSpeed(), 1e-9);
        assertEquals(1.1, builders.stats(foreman, job, crew).getSpeed(), 1e-9, "a Foreman is sped up by the other");
    }

    @Test
    void lonersPreferToWorkAlone() {
        final Settler loner = builder(SettlerRarity.COMMON, null, CampTraits.LONER);
        final Settler other = builder(SettlerRarity.COMMON, null);
        final Job job = job(JobKind.BUILD, Map.of(), Duration.ofMinutes(1));

        assertEquals(1.4, builders.stats(loner, job, List.of(loner)).getSpeed(), 1e-9);
        assertEquals(0.8, builders.stats(loner, job, List.of(loner, other)).getSpeed(), 1e-9);
    }

    @Test
    void situationalTraitsOnlyActOnTheirJobs() {
        final Settler patcher = builder(SettlerRarity.COMMON, null, CampTraits.PATCHER, CampTraits.TIRELESS);
        assertEquals(1.2, builders.stats(patcher, job(JobKind.REPAIR, Map.of(), Duration.ofMinutes(5)),
                List.of(patcher)).getSpeed(), 1e-9);
        assertEquals(1.15, builders.stats(patcher, job(JobKind.BUILD, Map.of(), Duration.ofHours(3)),
                List.of(patcher)).getSpeed(), 1e-9);
    }

    @Test
    void frugalAndPatcherCrewsHandBackPartOfTheCost() {
        final Settler frugal = builder(SettlerRarity.COMMON, null, CampTraits.FRUGAL);
        final Settler patcher = builder(SettlerRarity.LEGENDARY, null, CampTraits.PATCHER);

        assertEquals(0.05, builders.refund(job(JobKind.BUILD, Map.of(), Duration.ZERO), List.of(frugal, patcher)), 1e-9);
        assertEquals(0.45, builders.refund(job(JobKind.REPAIR, Map.of(), Duration.ZERO), List.of(frugal, patcher)), 1e-9);
    }

    @Test
    void aTieIsNoOneResource() {
        assertFalse(CampBuilders.mostly(job(JobKind.BUILD, Map.of("wood", 5, "stone", 5), Duration.ZERO), "wood"));
        assertTrue(CampBuilders.mostly(job(JobKind.BUILD, Map.of("wood", 6, "stone", 5), Duration.ZERO), "wood"));
        assertFalse(CampBuilders.mostly(job(JobKind.BUILD, Map.of(), Duration.ZERO), "wood"));
    }
}
