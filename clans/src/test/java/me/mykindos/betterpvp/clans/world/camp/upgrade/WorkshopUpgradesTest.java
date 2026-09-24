package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkshopUpgradesTest {

    private static final long CLAN = 5;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final long MINUTE = 60_000;

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final CampConfig config = mock(CampConfig.class);
    private CampUpgrades upgrades;
    private PlacedStructure workshop;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        upgrades = new CampUpgrades(store);
        workshop = new PlacedStructure(UUID.randomUUID(), CampStructures.WORKSHOP, new StructurePosition(0, 64, 0, 0),
                StructureCondition.ACTIVE);
        camp.getHolding().getStructures().add(workshop);
    }

    @Test
    void theSalvageBinRaisesTheRefundToItsShare() {
        final SalvageBin bin = new SalvageBin(upgrades, config);
        when(config.upgrade(CampStructures.WORKSHOP, SalvageBin.ID)).thenReturn(Optional.of(numbers(Map.of("refund", 0.5))));

        assertEquals(0.0, bin.refund(SITE, 0.0), "not fitted");

        workshop.getUpgrades().put(0, SalvageBin.ID);
        assertEquals(0.5, bin.refund(SITE, 0.0));
        assertEquals(0.75, bin.refund(SITE, 0.75), "a structure that already gives back more keeps its share");

        workshop.setCondition(StructureCondition.DISABLED);
        assertEquals(0.0, bin.refund(SITE, 0.0), "a disabled workshop loses it");
    }

    @Test
    void aRushCostsARateForEveryMinuteLeftWithAFloor() {
        assertEquals(100, RushOrder.price(2 * MINUTE, 20, 100));
        assertEquals(200, RushOrder.price(10 * MINUTE, 20, 100));
        assertEquals(220, RushOrder.price(10 * MINUTE + 1, 20, 100), "part of a minute counts as one");
    }

    @Test
    void aRushPricesAJobThatCannotMoveAtItsBasePace() {
        final Job job = Job.start(JobKind.BUILD, Duration.ofMinutes(30), ResourceCost.NONE, 0, 0);
        job.setRate(0, 0);

        assertEquals(30 * MINUTE, RushOrder.remainingMillis(job, 10 * MINUTE));
    }

    @Test
    void aRushWaitsADayBeforeTheNextOne() {
        final long day = Duration.ofDays(1).toMillis();

        assertEquals(0, RushOrder.cooldownLeft(0, 5_000), "never rushed");
        assertEquals(day - 1_000, RushOrder.cooldownLeft(10_000, 11_000));
        assertEquals(0, RushOrder.cooldownLeft(10_000, 10_000 + day));
    }

    @Test
    void anIdleStructureQueuesAnAdvanceWhileWorkingAndARepairWhileBroken() {
        final StructureType type = mock(StructureType.class);
        when(type.hasStage(1)).thenReturn(true);

        assertEquals(Optional.of(ConstructionAction.ADVANCE), BuildQueue.queueable(workshop, type));

        workshop.setCondition(StructureCondition.NEEDS_REPAIR);
        assertEquals(Optional.of(ConstructionAction.REPAIR), BuildQueue.queueable(workshop, type));

        workshop.setJob(Job.start(JobKind.REPAIR, Duration.ofMinutes(1), ResourceCost.NONE, 0, 0));
        assertTrue(BuildQueue.queueable(workshop, type).isEmpty(), "busy");

        workshop.setJob(null);
        workshop.setCondition(StructureCondition.ACTIVE);
        when(type.hasStage(1)).thenReturn(false);
        assertTrue(BuildQueue.queueable(workshop, type).isEmpty(), "no stage left");
    }

    @Test
    void onlyAJobOnAnotherStructureCountsAsBusyElsewhere() {
        final PlacedStructure barracks = new PlacedStructure(UUID.randomUUID(), CampStructures.BARRACKS,
                new StructurePosition(10, 64, 0, 0), StructureCondition.ACTIVE);
        camp.getHolding().getStructures().add(barracks);
        assertFalse(BuildQueue.busyElsewhere(camp.getHolding(), workshop.getId()));

        workshop.setJob(Job.start(JobKind.ADVANCE, Duration.ofMinutes(1), ResourceCost.NONE, 1, 0));
        assertFalse(BuildQueue.busyElsewhere(camp.getHolding(), workshop.getId()));
        assertTrue(BuildQueue.busyElsewhere(camp.getHolding(), barracks.getId()));
    }

    @Test
    void aCampHoldsOneQueuedActionUntilItIsCleared() {
        final BuildQueue queue = new BuildQueue(mock(Clans.class), upgrades, store, mock(Camps.class),
                mock(ClanManager.class), mock(ConstructionService.class), new StructureCatalogue());
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(queue.queue(player, SITE, workshop, ConstructionAction.ADVANCE));
        assertFalse(queue.queue(player, SITE, workshop, ConstructionAction.REPAIR), "one at a time");
        assertEquals(ConstructionAction.ADVANCE, camp.getQueuedAction().getAction());
        assertEquals(player.getUniqueId(), camp.getQueuedAction().getQueuedBy());

        final QueuedAction cleared = queue.clear(SITE);
        assertSame(ConstructionAction.ADVANCE, cleared.getAction());
        assertNull(camp.getQueuedAction());
        assertNull(queue.clear(SITE));
    }

    @Test
    void theWorkshopOffersEachUpgradeAtItsStage() {
        new SalvageBin(upgrades, config);
        new BuildQueue(mock(Clans.class), upgrades, store, mock(Camps.class), mock(ClanManager.class),
                mock(ConstructionService.class), new StructureCatalogue());

        assertTrue(upgrades.declared(CampStructures.WORKSHOP).stream()
                .anyMatch(declared -> declared.getId().equals(SalvageBin.ID) && declared.getStage() == 0));
        assertTrue(upgrades.declared(CampStructures.WORKSHOP).stream()
                .anyMatch(declared -> declared.getId().equals(BuildQueue.ID) && declared.getStage() == 1));
    }

    private static CampConfig.UpgradeNumbers numbers(Map<String, Object> settings) {
        return new CampConfig.UpgradeNumbers(ResourceCost.NONE, Duration.ZERO, 0, null, null, settings);
    }
}
