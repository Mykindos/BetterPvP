package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Types used throughout: every stage 0 costs 10 wood over 10 minutes and stage 1 costs 20 wood over 5 minutes. The
 * hall gives half back when demolished, the workshop needs a hall, the dock starts broken, the shed takes 3 minutes
 * and 2 wood to move and 4 minutes and 3 wood to repair, the keep can neither move nor be demolished, the well repairs
 * itself over 4 minutes, and the market is open to the public.
 */
class ConstructionServiceTest {

    private static final long MINUTE = 60_000;
    private static final SiteKey CAMP = SiteKey.of("camp", 7);

    private final AtomicLong now = new AtomicLong(1_000);
    private final List<Event> events = new ArrayList<>();
    private final FakeSite site = new FakeSite();
    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final FitCheck fitCheck = mock(FitCheck.class);
    private final Player player = mock(Player.class);

    private MockedStatic<Bukkit> bukkit;
    private World world;
    private SiteInstances instances;
    private ConstructionSites sites;
    private ConstructionChecks checks;
    private StructureStatusTracker tracker;
    private ConstructionService service;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

        world = mock(World.class);
        when(world.getName()).thenReturn("camp_7");

        instances = mock(SiteInstances.class);
        when(instances.byWorld("camp_7")).thenReturn(Optional.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY)));

        final StructureShapes shapes = mock(StructureShapes.class);
        final SchematicPlacement placement = SchematicPlacement.of(
                new Schematic(1, 1, 1, List.of(new Schematic.PlacedBlock(0, 0, 0, stone()))),
                new Location(world, 0, 64, 0), 0);
        when(shapes.placementOf(any(), anyString(), anyInt(), any())).thenReturn(Optional.of(placement));
        when(shapes.placementOf(any(), any(PlacedStructure.class))).thenReturn(Optional.of(placement));
        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        catalogue.register(new TestType("hall").refund(0.5));
        catalogue.register(new TestType("workshop").requires("hall"));
        catalogue.register(new TestType("dock").flags(StructureFlags.builder().startsBroken(true).build()));
        catalogue.register(new TestType("shed").move(3, 2).repair(4, 3));
        catalogue.register(new TestType("keep").flags(StructureFlags.builder().movable(false).demolishable(false).build()));
        catalogue.register(new TestType("well").repair(4, 3)
                .flags(StructureFlags.builder().selfRepairing(true).build()));
        catalogue.register(new TestType("market").flags(StructureFlags.builder().publicUse(true).build()));
        sites = new ConstructionSites(instances, catalogue);
        checks = new ConstructionChecks(sites, catalogue, shapes, fitCheck);
        tracker = new StructureStatusTracker(catalogue, now::get);
        service = new ConstructionService(sites, checks, tracker, shapes);
        sites.register("camp", site);
        site.balance.put("wood", 100);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void ac1_constructionNeedsARegisteredSiteWithItsHoldingLoaded() {
        final World elsewhere = mock(World.class);
        when(elsewhere.getName()).thenReturn("spawn");
        when(instances.byWorld("spawn")).thenReturn(Optional.empty());
        assertFalse(service.build(player, elsewhere, type("hall"), new Location(elsewhere, 0, 64, 0), 0).isSuccess());

        final World unregistered = mock(World.class);
        when(unregistered.getName()).thenReturn("arena_1");
        when(instances.byWorld("arena_1")).thenReturn(Optional.of(
                new SiteInstance(UUID.randomUUID(), SiteKey.of("arena", 1), "arena_1", SiteInstance.State.READY)));
        assertFalse(service.build(player, unregistered, type("hall"), new Location(unregistered, 0, 64, 0), 0)
                .isSuccess());

        final PlacedStructure hall = build("hall").getStructure();
        site.loaded = false;
        assertTrue(sites.worksite(world).isEmpty());
        assertFalse(build("hall").isSuccess());
        assertFalse(service.cancel(player, world, hall.getId()).isSuccess());
        assertFalse(service.claim(player, world, hall.getId()).isSuccess());
    }

    @Test
    void ac2_eachPlayerActionNeedsTheSitesPermissionForIt() {
        final PlacedStructure hall = finished("hall");
        final PlacedStructure dock = finished("dock");
        final PlacedStructure building = build("shed").getStructure();
        now.addAndGet(10 * MINUTE);
        final Location target = new Location(world, 30, 64, 0);

        assertRefusedOnlyWithout(ConstructionAction.BUILD, () -> build("hall"));
        assertRefusedOnlyWithout(ConstructionAction.MOVE, () -> service.move(player, world, hall.getId(), target, 0));
        assertRefusedOnlyWithout(ConstructionAction.PICK_UPGRADE,
                () -> service.upgrade(player, world, hall.getId(), "lantern"));
        assertRefusedOnlyWithout(ConstructionAction.REPAIR, () -> service.repair(player, world, dock.getId()));
        assertRefusedOnlyWithout(ConstructionAction.CLAIM, () -> service.claim(player, world, building.getId()));
        assertRefusedOnlyWithout(ConstructionAction.ADVANCE, () -> service.advance(player, world, hall.getId()));
        assertRefusedOnlyWithout(ConstructionAction.CANCEL, () -> service.cancel(player, world, hall.getId()));
        assertRefusedOnlyWithout(ConstructionAction.DEMOLISH, () -> service.demolish(player, world, hall.getId()));
    }

    @Test
    void ac2_theSitesOwnActionsCheckNoPermission() {
        final PlacedStructure hall = finished("hall");
        final PlacedStructure dock = finished("dock");
        site.denied.addAll(EnumSet.allOf(ConstructionAction.class));

        assertTrue(service.advance(world, hall.getId()).isSuccess());
        assertTrue(service.repair(world, dock.getId()).isSuccess());
        assertTrue(service.finish(world, hall.getId()).isSuccess());
        final PlacedStructure shed = service.grant(sites.worksite(world).orElseThrow(), type("shed"),
                new StructurePosition(40, 64, 0, 0), StructureCondition.ACTIVE);
        assertEquals(new StructurePosition(40, 64, 0, 0),
                site.holding.find(shed.getId()).orElseThrow().getPosition());
    }

    @Test
    void ac3_buildingIsRefusedUntilWhatItNeedsIsFinished() {
        assertFalse(build("workshop").isSuccess());

        final PlacedStructure hall = build("hall").getStructure();
        assertFalse(build("workshop").isSuccess(), "an unfinished hall does not count");

        now.addAndGet(10 * MINUTE);
        service.claim(player, world, hall.getId());
        assertTrue(build("workshop").isSuccess());
    }

    @Test
    void ac3_buildingIsRefusedByTheSitesOwnGateOrWithoutEnoughResources() {
        site.gate = Component.text("needs a bigger hall");
        assertFalse(build("hall").isSuccess());
        assertTrue(checks.unavailable(player, CAMP, type("hall")).isPresent());

        site.gate = null;
        site.balance.put("wood", 5);
        assertFalse(build("hall").isSuccess());
        assertEquals(5, site.balance.get("wood"));
        assertTrue(site.holding.getStructures().isEmpty());
    }

    @Test
    void ac3_availabilityGivesTheSameReasonsWithoutTheWorld() {
        final StructureType workshop = type("workshop");
        assertTrue(checks.unavailable(player, CAMP, workshop).isPresent(), "needs a hall first");

        finished("hall");
        assertTrue(checks.unavailable(player, CAMP, workshop).isEmpty());

        site.balance.put("wood", 0);
        assertTrue(checks.unavailable(player, CAMP, workshop).isPresent(), "cannot afford it");

        site.balance.put("wood", 100);
        site.denied.add(ConstructionAction.BUILD);
        assertTrue(checks.unavailable(player, CAMP, workshop).isPresent(), "not allowed");
    }

    @Test
    void ac4_buildingIsRefusedWhereItDoesNotFit() {
        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.of(Component.text("overlaps")));

        assertFalse(build("hall").isSuccess());
        assertEquals(100, site.balance.get("wood"));
    }

    @Test
    void ac4_aMoveOrAnAdvanceIgnoresTheStructureItself() {
        final PlacedStructure hall = finished("hall");

        service.move(player, world, hall.getId(), new Location(world, 30, 64, 0), 0);
        service.advance(player, world, hall.getId());

        verify(fitCheck, Mockito.times(2)).problem(eq(world), any(), any(), any(), eq(hall.getId()));
        verify(fitCheck, Mockito.times(1)).problem(eq(world), any(), any(), any(), isNull());
    }

    @Test
    void ac5_buildingPaysAndStartsABuildJob() {
        final ConstructionResult result = build("hall");

        assertTrue(result.isSuccess());
        assertEquals(90, site.balance.get("wood"));
        final PlacedStructure hall = result.getStructure();
        assertEquals(StructureCondition.UNDER_CONSTRUCTION, hall.getCondition());
        assertEquals(StructureStatus.UNDER_CONSTRUCTION, hall.status(now.get()));
        assertEquals(JobKind.BUILD, hall.getJob().getKind());
        assertEquals(10 * MINUTE, hall.getJob().remainingMillis(now.get()));
        assertTrue(events.stream().anyMatch(event -> event instanceof StructurePlacedEvent));
    }

    @Test
    void ac6_aJobMovesOnWithNothingTickingIt() {
        final PlacedStructure hall = build("hall").getStructure();

        now.addAndGet(4 * MINUTE);

        assertEquals(0.4, hall.getJob().progress(now.get()), 1e-9);
        now.addAndGet(6 * MINUTE);
        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()));
    }

    @Test
    void ac7_aJobRuleHoldsWorkAndLetsItGoAgain() {
        final boolean[] siege = {false};
        site.rules.add(rule("siege", () -> siege[0], 1.0));
        final PlacedStructure hall = build("hall").getStructure();
        final Worksite worksite = sites.worksite(world).orElseThrow();

        siege[0] = true;
        tracker.refresh(worksite);
        assertEquals(StructureStatus.PAUSED, hall.status(now.get()));

        now.addAndGet(60 * MINUTE);
        siege[0] = false;
        tracker.refresh(worksite);
        assertEquals(StructureStatus.UNDER_CONSTRUCTION, hall.status(now.get()), "time under siege did not count");
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureStatusChangeEvent change
                && change.getTo() == StructureStatus.PAUSED));
    }

    @Test
    void ac7_ratesFromEveryRuleMultiply() {
        site.rules.add(rule("crew", () -> false, 2.0));
        site.rules.add(rule("tools", () -> false, 1.5));

        final PlacedStructure hall = build("hall").getStructure();

        assertEquals(3.0, hall.getJob().getRate(), 1e-9);
        now.addAndGet(2 * MINUTE);
        assertEquals(0.6, hall.getJob().progress(now.get()), 1e-9);
    }

    @Test
    void ac8_aFinishedJobIsNeverHeldOrSlowedAgain() {
        final boolean[] siege = {false};
        final double[] rate = {1.0};
        site.rules.add(rule("siege", () -> siege[0], 1.0));
        site.rules.add(new JobRule() {
            @Override
            public @NotNull String id() {
                return "crew";
            }

            @Override
            public boolean holds(@NotNull SiteKey key, @NotNull PlacedStructure structure, @NotNull Job job) {
                return false;
            }

            @Override
            public double rate(@NotNull SiteKey key, @NotNull PlacedStructure structure, @NotNull Job job) {
                return rate[0];
            }
        });
        final PlacedStructure hall = build("hall").getStructure();
        final Worksite worksite = sites.worksite(world).orElseThrow();

        now.addAndGet(60 * MINUTE);
        siege[0] = true;
        rate[0] = 0;
        tracker.refresh(worksite);

        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()), "its crew leaving must not stop the claim");
        assertEquals(1.0, hall.getJob().getRate(), 1e-9);
    }

    @Test
    void ac9_aJobCanOnlyBeClaimedOnceItIsDone() {
        final PlacedStructure hall = build("hall").getStructure();
        assertFalse(service.claim(player, world, hall.getId()).isSuccess());

        now.addAndGet(10 * MINUTE);
        assertTrue(service.claim(player, world, hall.getId()).isSuccess());

        assertEquals(StructureCondition.ACTIVE, hall.getCondition());
        assertNull(hall.getJob());
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureClaimedEvent claimed
                && claimed.getStructure() == hall && claimed.getJob().getKind() == JobKind.BUILD
                && claimed.getWorld() == world));
    }

    @Test
    void ac9_aHeldJobCannotBeClaimed() {
        site.rules.add(rule("siege", () -> true, 1.0));
        final PlacedStructure hall = build("hall").getStructure();
        now.addAndGet(60 * MINUTE);

        assertFalse(service.claim(player, world, hall.getId()).isSuccess());
    }

    @Test
    void ac9_somethingThatStartsBrokenNeedsARepairOnceBuilt() {
        final PlacedStructure dock = finished("dock");

        assertEquals(StructureCondition.NEEDS_REPAIR, dock.getCondition());
    }

    @Test
    void ac9_claimingEachKindOfJobFinishesItsOwnWork() {
        final PlacedStructure hall = finished("hall");
        service.advance(player, world, hall.getId());
        now.addAndGet(5 * MINUTE);
        assertTrue(service.claim(player, world, hall.getId()).isSuccess());
        assertEquals(1, hall.getStage());

        final PlacedStructure shed = finished("shed");
        service.move(player, world, shed.getId(), new Location(world, 30, 64, 5), 1);
        now.addAndGet(3 * MINUTE);
        assertTrue(service.claim(player, world, shed.getId()).isSuccess());
        assertEquals(new StructurePosition(30, 64, 5, 1), shed.getPosition());

        shed.setCondition(StructureCondition.NEEDS_REPAIR);
        service.repair(player, world, shed.getId());
        now.addAndGet(4 * MINUTE);
        assertTrue(service.claim(player, world, shed.getId()).isSuccess());
        assertEquals(StructureCondition.ACTIVE, shed.getCondition());

        service.upgrade(player, world, shed.getId(), "bell");
        now.addAndGet(5 * MINUTE);
        assertTrue(service.claim(player, world, shed.getId()).isSuccess());
        assertEquals(Optional.of("bell"), shed.upgradeAt(0));
        assertEquals(6, events.stream().filter(event -> event instanceof StructureClaimedEvent).count(),
                "the two builds, the advance, the move, the repair and the upgrade");
    }

    @Test
    void ac10_cancellingAnUnfinishedBuildGivesEverythingBackAndRemovesIt() {
        final PlacedStructure hall = build("hall").getStructure();

        assertTrue(service.cancel(player, world, hall.getId()).isSuccess());

        assertEquals(100, site.balance.get("wood"));
        assertTrue(site.holding.getStructures().isEmpty());
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureRemovedEvent removed
                && !removed.isDemolished()));
    }

    @Test
    void ac10_cancellingAnyOtherJobRefundsItAndLeavesTheStructureAsItWas() {
        final PlacedStructure hall = finished("hall");
        service.advance(player, world, hall.getId());
        assertEquals(70, site.balance.get("wood"));

        assertTrue(service.cancel(player, world, hall.getId()).isSuccess());

        assertEquals(90, site.balance.get("wood"));
        assertEquals(0, hall.getStage());
        assertEquals(StructureStatus.ACTIVE, hall.status(now.get()));
        assertTrue(site.holding.find(hall.getId()).isPresent());
    }

    @Test
    void ac10_cancellingAnUpgradeGivesItsCostBackAndLeavesTheStageOpen() {
        final PlacedStructure hall = finished("hall");
        service.upgrade(player, world, hall.getId(), "bell");

        assertTrue(service.cancel(player, world, hall.getId()).isSuccess());

        assertEquals(90, site.balance.get("wood"));
        assertTrue(hall.upgradeAt(0).isEmpty());
        assertTrue(service.upgrade(player, world, hall.getId(), "lantern").isSuccess());
    }

    @Test
    void ac10_aFinishedJobCannotBeCancelled() {
        final PlacedStructure hall = build("hall").getStructure();
        now.addAndGet(10 * MINUTE);

        assertFalse(service.cancel(player, world, hall.getId()).isSuccess());

        assertEquals(90, site.balance.get("wood"));
        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()));
    }

    @Test
    void ac11_anInstantMoveTakesEffectStraightAway() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.move(player, world, hall.getId(), new Location(world, 30, 64, 5), 2).isSuccess());

        assertEquals(new StructurePosition(30, 64, 5, 2), hall.getPosition());
        assertNull(hall.getJob());
    }

    @Test
    void ac11_aTimedMoveIsAJobThatPaysItsCost() {
        final PlacedStructure shed = finished("shed");

        assertTrue(service.move(player, world, shed.getId(), new Location(world, 30, 64, 5), 0).isSuccess());

        assertEquals(88, site.balance.get("wood"));
        assertEquals(JobKind.MOVE, shed.getJob().getKind());
        assertEquals(new StructurePosition(0, 64, 0, 0), shed.getPosition(), "not until it is claimed");
    }

    @Test
    void ac11_aMoveIsRefusedForAFixedTypeOrWhereItDoesNotFit() {
        final PlacedStructure keep = finished("keep");
        final PlacedStructure hall = finished("hall");
        final Location target = new Location(world, 30, 64, 5);

        assertFalse(service.move(player, world, keep.getId(), target, 0).isSuccess());

        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.of(Component.text("overlaps")));
        assertFalse(service.move(player, world, hall.getId(), target, 0).isSuccess());
        assertTrue(checks.moveProblem(player, world, hall.getId(), target, 0).isPresent());
    }

    @Test
    void ac11_placingAStructureThatWasNotPlacedIsInstantAndKeepsItsStorage() {
        final PlacedStructure shed = finished("shed");
        shed.setCondition(StructureCondition.NOT_PLACED);
        shed.setStorage(Map.of("1,0,0", List.of("c3RvbmU=")));

        assertTrue(service.move(player, world, shed.getId(), new Location(world, 30, 64, 5), 0).isSuccess());

        assertEquals(StructureCondition.ACTIVE, shed.getCondition());
        assertNull(shed.getJob());
        assertEquals(new StructurePosition(30, 64, 5, 0), shed.getPosition());
        assertEquals(Map.of("1,0,0", List.of("c3RvbmU=")), shed.getStorage());
    }

    @Test
    void ac12_advancingRaisesTheStageOnceClaimed() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.advance(player, world, hall.getId()).isSuccess());
        assertEquals(0, hall.getStage(), "not until it is claimed");
        assertEquals(StructureStatus.ADVANCING, hall.status(now.get()));
        assertFalse(hall.status(now.get()).isUsable());

        now.addAndGet(5 * MINUTE);
        service.claim(player, world, hall.getId());
        assertEquals(1, hall.getStage());
        assertFalse(service.advance(player, world, hall.getId()).isSuccess(), "there is no stage after that");
    }

    @Test
    void ac12_advancingNeedsAnIdleActiveStructureThatFitsAndIsPaidFor() {
        final PlacedStructure dock = finished("dock");
        assertFalse(service.advance(player, world, dock.getId()).isSuccess(), "it needs a repair first");

        final PlacedStructure hall = finished("hall");
        service.upgrade(player, world, hall.getId(), "bell");
        assertFalse(service.advance(player, world, hall.getId()).isSuccess(), "a job is running");
        service.cancel(player, world, hall.getId());

        site.gate = Component.text("needs a bigger hall");
        assertFalse(service.advance(player, world, hall.getId()).isSuccess());
        site.gate = null;

        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.of(Component.text("overlaps")));
        assertFalse(service.advance(player, world, hall.getId()).isSuccess());
        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        site.balance.put("wood", 5);
        assertFalse(service.advance(world, hall.getId()).isSuccess());
        assertNull(hall.getJob());
    }

    @Test
    void ac13_anInstantRepairMakesItActiveAtOnce() {
        final PlacedStructure dock = finished("dock");

        assertTrue(service.repair(player, world, dock.getId()).isSuccess());

        assertEquals(StructureCondition.ACTIVE, dock.getCondition());
    }

    @Test
    void ac13_aTimedRepairIsAJobThatPaysItsCost() {
        final PlacedStructure shed = finished("shed");
        assertFalse(service.repair(player, world, shed.getId()).isSuccess(), "nothing to repair");

        shed.setCondition(StructureCondition.DISABLED);
        assertTrue(service.repair(player, world, shed.getId()).isSuccess());

        assertEquals(87, site.balance.get("wood"));
        assertEquals(JobKind.REPAIR, shed.getJob().getKind());
        assertFalse(service.repair(player, world, shed.getId()).isSuccess(), "already under repair");
    }

    @Test
    void ac14_theSiteCanDisableAStandingStructure() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.disable(world, hall.getId()).isSuccess());

        assertEquals(StructureStatus.DISABLED, hall.status(now.get()));
        assertFalse(hall.status(now.get()).isUsable());
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureStatusChangeEvent change
                && change.getTo() == StructureStatus.DISABLED));
        assertTrue(service.repair(player, world, hall.getId()).isSuccess());
    }

    @Test
    void ac14_onlyAStandingStructureCanBeDisabled() {
        final PlacedStructure building = build("hall").getStructure();
        assertFalse(service.disable(world, building.getId()).isSuccess());

        final PlacedStructure shed = finished("shed");
        shed.setCondition(StructureCondition.NOT_PLACED);
        assertFalse(service.disable(world, shed.getId()).isSuccess());
    }

    @Test
    void ac15_aSelfRepairingStructureComesBackOnItsOwnForNothing() {
        final PlacedStructure well = finished("well");
        final Worksite worksite = sites.worksite(world).orElseThrow();
        service.disable(world, well.getId());

        now.addAndGet(4 * MINUTE - 1);
        tracker.refresh(worksite);
        assertEquals(StructureStatus.DISABLED, well.status(now.get()));

        now.addAndGet(1);
        tracker.refresh(worksite);
        assertEquals(StructureCondition.ACTIVE, well.getCondition());
        assertNull(well.getJob());
        assertEquals(90, site.balance.get("wood"), "only its build was paid for");
    }

    @Test
    void ac15_aPaidRepairUnderWayFinishesTheJobInstead() {
        final PlacedStructure well = finished("well");
        service.disable(world, well.getId());
        service.repair(player, world, well.getId());

        now.addAndGet(4 * MINUTE);
        tracker.refresh(sites.worksite(world).orElseThrow());

        assertEquals(StructureCondition.DISABLED, well.getCondition(), "the paid repair is claimed as usual");
        assertEquals(StructureStatus.READY_TO_CLAIM, well.status(now.get()));
        assertTrue(service.claim(player, world, well.getId()).isSuccess());
        assertEquals(StructureCondition.ACTIVE, well.getCondition());
    }

    @Test
    void ac15_otherStructuresStayDisabledUntilRepaired() {
        final PlacedStructure shed = finished("shed");
        service.disable(world, shed.getId());

        now.addAndGet(60 * MINUTE);
        tracker.refresh(sites.worksite(world).orElseThrow());

        assertEquals(StructureCondition.DISABLED, shed.getCondition());
    }

    @Test
    void ac1_aDisabledStructureStaysDisabledThroughAnAdvanceMoveOrUpgradeAndItsClaim() {
        final PlacedStructure advancing = finished("hall");
        final PlacedStructure moving = finished("shed");
        final PlacedStructure upgrading = finished("hall");
        service.advance(player, world, advancing.getId());
        service.move(player, world, moving.getId(), new Location(world, 30, 64, 5), 0);
        service.upgrade(player, world, upgrading.getId(), "bell");
        final List<PlacedStructure> busy = List.of(advancing, moving, upgrading);
        events.clear();

        for (PlacedStructure structure : busy) {
            assertTrue(service.disable(world, structure.getId()).isSuccess());
            assertEquals(StructureStatus.DISABLED, structure.status(now.get()));
            assertFalse(structure.status(now.get()).isUsable());
        }
        assertEquals(3, events.stream().filter(event -> event instanceof StructureStatusChangeEvent change
                && change.getTo() == StructureStatus.DISABLED).count());

        now.addAndGet(5 * MINUTE);
        for (PlacedStructure structure : busy) {
            assertTrue(service.claim(player, world, structure.getId()).isSuccess());
            assertEquals(StructureCondition.DISABLED, structure.getCondition());
            assertEquals(StructureStatus.DISABLED, structure.status(now.get()));
        }
        assertEquals(1, advancing.getStage());
        assertEquals(new StructurePosition(30, 64, 5, 0), moving.getPosition());
        assertTrue(upgrading.hasUpgrade("bell"));
    }

    @Test
    void ac2_anAdvanceOrUpgradeLeavesSelfRepairAlone() {
        final PlacedStructure advancing = finished("well");
        final PlacedStructure upgrading = finished("well");
        service.advance(player, world, advancing.getId());
        service.upgrade(player, world, upgrading.getId(), "bell");
        service.disable(world, advancing.getId());
        service.disable(world, upgrading.getId());

        now.addAndGet(4 * MINUTE);
        tracker.refresh(sites.worksite(world).orElseThrow());

        assertEquals(StructureCondition.ACTIVE, advancing.getCondition());
        assertEquals(StructureCondition.ACTIVE, upgrading.getCondition());
        assertEquals(JobKind.ADVANCE, advancing.getJob().getKind(), "the advance carries on");
        assertEquals(JobKind.FIT_UPGRADE, upgrading.getJob().getKind(), "the upgrade carries on");
    }

    @Test
    void ac2_aRunningRepairStopsSelfRepair() {
        final PlacedStructure well = finished("well");
        site.rules.add(rule("siege", () -> true, 1.0));
        service.disable(world, well.getId());
        service.repair(player, world, well.getId());

        now.addAndGet(60 * MINUTE);
        tracker.refresh(sites.worksite(world).orElseThrow());

        assertEquals(StructureCondition.DISABLED, well.getCondition());
        assertEquals(JobKind.REPAIR, well.getJob().getKind());
    }

    @Test
    void ac16_anInstantUpgradeIsFittedAtOnceAndTakesItsStagesPick() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.upgrade(player, world, hall.getId(), "lantern").isSuccess());

        assertTrue(hall.hasUpgrade("lantern"));
        assertEquals(85, site.balance.get("wood"));
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureUpgradedEvent upgraded
                && upgraded.getUpgrade().getId().equals("lantern")));
        assertFalse(service.upgrade(player, world, hall.getId(), "bell").isSuccess(), "one pick per stage");
        assertEquals(85, site.balance.get("wood"));
        assertTrue(checks.upgradeUnavailable(player, CAMP, hall.getId(), "bell").isPresent());
    }

    @Test
    void ac16_aTimedUpgradeIsFittedOnlyOnceClaimed() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.upgrade(player, world, hall.getId(), "bell").isSuccess());
        assertFalse(hall.hasUpgrade("bell"));
        assertEquals(StructureStatus.ACTIVE, hall.status(now.get()), "it stays usable while the upgrade goes in");

        now.addAndGet(5 * MINUTE);
        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()));
        assertTrue(service.claim(player, world, hall.getId()).isSuccess());
        assertEquals(Optional.of("bell"), hall.upgradeAt(0));
        assertNull(hall.getJob());
    }

    @Test
    void ac16_aLaterStagesUpgradeWaitsForItAndAnEarlierStageCanStillBePicked() {
        final PlacedStructure hall = finished("hall");
        assertFalse(service.upgrade(player, world, hall.getId(), "tower").isSuccess(), "not reached yet");

        service.advance(player, world, hall.getId());
        now.addAndGet(5 * MINUTE);
        service.claim(player, world, hall.getId());

        assertTrue(service.upgrade(player, world, hall.getId(), "tower").isSuccess());
        assertTrue(service.upgrade(player, world, hall.getId(), "lantern").isSuccess(), "stage 1 was never picked from");
        assertEquals(2, hall.getUpgrades().size());
    }

    @Test
    void ac16_upgradesStayThroughAnAdvance() {
        final PlacedStructure hall = finished("hall");
        service.upgrade(player, world, hall.getId(), "lantern");

        service.advance(player, world, hall.getId());
        now.addAndGet(5 * MINUTE);
        service.claim(player, world, hall.getId());

        assertTrue(hall.hasUpgrade("lantern"));
    }

    @Test
    void ac16_onlyAnActiveStructureCanTakeAnUpgrade() {
        final PlacedStructure dock = finished("dock");

        assertFalse(service.upgrade(player, world, dock.getId(), "lantern").isSuccess());
        assertTrue(checks.upgradeUnavailable(player, CAMP, dock.getId(), "lantern").isPresent());
    }

    @Test
    void ac18_demolishingGivesBackItsShareAndDropsWhatItHeldOnceItIsGone() {
        final PlacedStructure hall = finished("hall");
        assertEquals(90, site.balance.get("wood"));

        assertTrue(service.demolish(player, world, hall.getId()).isSuccess());

        assertEquals(95, site.balance.get("wood"), "half of the 10 it cost");
        assertTrue(site.holding.find(hall.getId()).isEmpty());
        assertEquals(1, site.dropsSeenWithoutTheStructure,
                "contents drop after the structure has left the holding");
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureRemovedEvent removed
                && removed.isDemolished()));
    }

    @Test
    void ac18_theSiteDecidesTheShareDemolishingGivesBack() {
        final PlacedStructure hall = finished("hall");
        site.refundShare = 1.0;

        assertEquals(10, sites.demolishRefund(CAMP, hall, type("hall")).getAmounts().get("wood"));
        assertTrue(service.demolish(player, world, hall.getId()).isSuccess());
        assertEquals(100, site.balance.get("wood"), "all of the 10 it cost");
    }

    @Test
    void ac18_demolishingIsRefusedForAFixedTypeAnUnfinishedOrPutAwayStructureOrOneWithAJob() {
        final PlacedStructure keep = finished("keep");
        assertFalse(service.demolish(player, world, keep.getId()).isSuccess());

        final PlacedStructure building = build("shed").getStructure();
        assertFalse(service.demolish(player, world, building.getId()).isSuccess());

        final PlacedStructure hall = finished("hall");
        service.advance(player, world, hall.getId());
        assertFalse(service.demolish(player, world, hall.getId()).isSuccess());

        final PlacedStructure stored = finished("market");
        stored.setCondition(StructureCondition.NOT_PLACED);
        assertFalse(service.demolish(player, world, stored.getId()).isSuccess());
        assertEquals(4, site.holding.getStructures().size());
    }

    @Test
    void ac19_finishingAJobMakesItReadyToClaimEvenWhileHeld() {
        site.rules.add(rule("siege", () -> true, 1.0));
        final PlacedStructure hall = build("hall").getStructure();
        assertEquals(StructureStatus.PAUSED, hall.status(now.get()));

        assertTrue(service.finish(world, hall.getId()).isSuccess());

        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()));
        assertEquals(90, site.balance.get("wood"), "nothing more is paid");
        assertTrue(service.claim(player, world, hall.getId()).isSuccess());
        assertEquals(StructureCondition.ACTIVE, hall.getCondition());
    }

    @Test
    void ac19_thereIsNothingToFinishWithoutAnUnfinishedJob() {
        final PlacedStructure hall = finished("hall");
        assertFalse(service.finish(world, hall.getId()).isSuccess());

        service.advance(player, world, hall.getId());
        now.addAndGet(5 * MINUTE);
        assertFalse(service.finish(world, hall.getId()).isSuccess(), "already done");
    }

    @Test
    void ac20_grantingSkipsEveryCheckAndCostsNothing() {
        site.balance.put("wood", 0);
        site.denied.addAll(EnumSet.allOf(ConstructionAction.class));
        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.of(Component.text("overlaps")));

        final PlacedStructure workshop = service.grant(sites.worksite(world).orElseThrow(), type("workshop"),
                new StructurePosition(5, 64, 5, 0), StructureCondition.ACTIVE);

        assertEquals(new StructurePosition(5, 64, 5, 0),
                site.holding.find(workshop.getId()).orElseThrow().getPosition());
        assertEquals(0, site.balance.get("wood"));
    }

    @Test
    void ac22_everyStatusChangeIsAnnouncedAndEveryChangeIsWrittenDown() {
        final PlacedStructure hall = build("hall").getStructure();
        final int afterBuild = site.changes;
        assertTrue(afterBuild > 0);

        now.addAndGet(10 * MINUTE);
        service.claim(player, world, hall.getId());

        assertTrue(site.changes > afterBuild);
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureStatusChangeEvent change
                && change.getStructure() == hall && change.getFrom() == StructureStatus.UNDER_CONSTRUCTION
                && change.getTo() == StructureStatus.ACTIVE));
    }

    @Test
    void ac24_aPublicStructureCanBeUsedByAnyoneAndAnyOtherOnlyByMembers() {
        final PlacedStructure market = finished("market");
        final PlacedStructure hall = finished("hall");
        final Player visitor = mock(Player.class);
        site.members.add(player);

        assertTrue(sites.canUse(visitor, CAMP, market));
        assertFalse(sites.canUse(visitor, CAMP, hall));
        assertTrue(sites.canUse(player, CAMP, market));
        assertTrue(sites.canUse(player, CAMP, hall));
    }

    @Test
    void ac2_buildAndAvailabilityAgreeWhenTheHoldingIsNotLoaded() {
        site.loaded = false;

        assertSameReason(checks.unavailable(player, CAMP, type("hall")), build("hall"));
    }

    @Test
    void ac2_buildAndAvailabilityAgreeWhenTheSiteIsNotRegistered() {
        final World arena = unregisteredWorld();

        assertSameReason(checks.unavailable(player, SiteKey.of("arena", 1), type("hall")),
                service.build(player, arena, type("hall"), new Location(arena, 0, 64, 0), 0));
    }

    @Test
    void ac2_buildAndAvailabilityAgreeOnEveryOtherRefusal() {
        site.denied.add(ConstructionAction.BUILD);
        assertSameReason(checks.unavailable(player, CAMP, type("hall")), build("hall"));
        site.denied.clear();

        assertSameReason(checks.unavailable(player, CAMP, type("workshop")), build("workshop"));

        site.gate = Component.text("needs a bigger hall");
        assertSameReason(checks.unavailable(player, CAMP, type("hall")), build("hall"));
        site.gate = null;

        site.balance.put("wood", 5);
        assertSameReason(checks.unavailable(player, CAMP, type("hall")), build("hall"));
    }

    @Test
    void ac2_upgradeAndAvailabilityAgreeWhenDeniedAndTheUpgradeIsUnknown() {
        final PlacedStructure hall = finished("hall");
        site.denied.add(ConstructionAction.PICK_UPGRADE);

        assertSameReason(checks.upgradeUnavailable(player, CAMP, hall.getId(), "moat"),
                service.upgrade(player, world, hall.getId(), "moat"));
    }

    @Test
    void ac2_upgradeAndAvailabilityAgreeWhenTheHoldingIsNotLoaded() {
        final PlacedStructure hall = finished("hall");
        site.loaded = false;

        assertSameReason(checks.upgradeUnavailable(player, CAMP, hall.getId(), "lantern"),
                service.upgrade(player, world, hall.getId(), "lantern"));
    }

    @Test
    void ac2_upgradeAndAvailabilityAgreeWhenTheSiteIsNotRegistered() {
        final World arena = unregisteredWorld();
        final UUID id = UUID.randomUUID();

        assertSameReason(checks.upgradeUnavailable(player, SiteKey.of("arena", 1), id, "lantern"),
                service.upgrade(player, arena, id, "lantern"));
    }

    @Test
    void ac2_upgradeAndAvailabilityAgreeWhenTheStructuresTypeIsUnknown() {
        final PlacedStructure ruin = new PlacedStructure(UUID.randomUUID(), "ruin", new StructurePosition(0, 64, 0, 0),
                StructureCondition.ACTIVE);
        site.holding.getStructures().add(ruin);

        assertSameReason(checks.upgradeUnavailable(player, CAMP, ruin.getId(), "lantern"),
                service.upgrade(player, world, ruin.getId(), "lantern"));
    }

    @Test
    void ac2_upgradeAndAvailabilityAgreeOnEveryOtherRefusal() {
        final PlacedStructure hall = finished("hall");
        final PlacedStructure dock = finished("dock");

        site.denied.add(ConstructionAction.PICK_UPGRADE);
        assertSameReason(checks.upgradeUnavailable(player, CAMP, hall.getId(), "lantern"),
                service.upgrade(player, world, hall.getId(), "lantern"));
        site.denied.clear();

        final UUID missing = UUID.randomUUID();
        assertSameReason(checks.upgradeUnavailable(player, CAMP, missing, "lantern"),
                service.upgrade(player, world, missing, "lantern"));
        assertSameReason(checks.upgradeUnavailable(player, CAMP, hall.getId(), "moat"),
                service.upgrade(player, world, hall.getId(), "moat"));
        assertSameReason(checks.upgradeUnavailable(player, CAMP, hall.getId(), "tower"),
                service.upgrade(player, world, hall.getId(), "tower"));
        assertSameReason(checks.upgradeUnavailable(player, CAMP, dock.getId(), "lantern"),
                service.upgrade(player, world, dock.getId(), "lantern"));

        site.balance.put("wood", 0);
        assertSameReason(checks.upgradeUnavailable(player, CAMP, hall.getId(), "lantern"),
                service.upgrade(player, world, hall.getId(), "lantern"));
    }

    private World unregisteredWorld() {
        final World arena = mock(World.class);
        when(arena.getName()).thenReturn("arena_1");
        when(instances.byWorld("arena_1")).thenReturn(Optional.of(
                new SiteInstance(UUID.randomUUID(), SiteKey.of("arena", 1), "arena_1", SiteInstance.State.READY)));
        return arena;
    }

    private static void assertSameReason(Optional<Component> query, ConstructionResult action) {
        assertFalse(action.isSuccess(), "the action is refused");
        assertTrue(query.isPresent(), "the query refuses too");
        assertEquals(query, Optional.ofNullable(action.getReason()));
    }

    private void assertRefusedOnlyWithout(ConstructionAction action, Supplier<ConstructionResult> attempt) {
        site.denied.add(action);
        assertFalse(attempt.get().isSuccess(), action + " is refused without its permission");
        site.denied.remove(action);
        assertTrue(attempt.get().isSuccess(), action + " goes through with its permission");
    }

    private StructureType type(String id) {
        return catalogue.find(id).orElseThrow();
    }

    private ConstructionResult build(String type) {
        return service.build(player, world, type(type), new Location(world, 0, 64, 0), 0);
    }

    private PlacedStructure finished(String type) {
        final PlacedStructure structure = build(type).getStructure();
        now.addAndGet(10 * MINUTE);
        service.claim(player, world, structure.getId());
        return structure;
    }

    private static JobRule rule(String id, Supplier<Boolean> holds, double rate) {
        return new JobRule() {
            @Override
            public @NotNull String id() {
                return id;
            }

            @Override
            public boolean holds(@NotNull SiteKey key, @NotNull PlacedStructure structure, @NotNull Job job) {
                return holds.get();
            }

            @Override
            public double rate(@NotNull SiteKey key, @NotNull PlacedStructure structure, @NotNull Job job) {
                return rate;
            }
        };
    }

    private static BlockData stone() {
        final BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(Material.STONE);
        return data;
    }

    private final class FakeSite implements ConstructionSite, ResourceLedger {

        private final Holding holding = new Holding();
        private final Map<String, Integer> balance = new HashMap<>();
        private final List<JobRule> rules = new ArrayList<>();
        private final Set<ConstructionAction> denied = EnumSet.noneOf(ConstructionAction.class);
        private final List<Player> members = new ArrayList<>();
        private boolean loaded = true;
        private @Nullable Component gate;
        private double refundShare = -1;
        private int changes;
        private int dropsSeenWithoutTheStructure;

        @Override
        public @NotNull Optional<Holding> holding(@NotNull SiteKey key) {
            return loaded ? Optional.of(holding) : Optional.empty();
        }

        @Override
        public void changed(@NotNull SiteKey key) {
            changes++;
        }

        @Override
        public @NotNull ResourceLedger ledger() {
            return this;
        }

        @Override
        public boolean allows(@NotNull Player player, @NotNull SiteKey key, @NotNull ConstructionAction action) {
            return !denied.contains(action);
        }

        @Override
        public boolean isMember(@NotNull Player player, @NotNull SiteKey key) {
            return members.contains(player);
        }

        @Override
        public @NotNull Optional<Component> blocked(@NotNull SiteKey key, @NotNull Holding holding,
                                                    @NotNull StructureType type, int stage) {
            return Optional.ofNullable(gate);
        }

        @Override
        public @NotNull List<JobRule> jobRules() {
            return rules;
        }

        @Override
        public double demolishRefund(@NotNull SiteKey key, @NotNull PlacedStructure structure,
                                     @NotNull StructureType type) {
            return refundShare < 0 ? ConstructionSite.super.demolishRefund(key, structure, type) : refundShare;
        }

        @Override
        public @NotNull List<StructureContents> contents() {
            return List.of((key, structure, at) -> {
                if (holding.find(structure.getId()).isEmpty()) {
                    dropsSeenWithoutTheStructure++;
                }
            });
        }

        @Override
        public boolean canAfford(@NotNull SiteKey key, @NotNull ResourceCost cost) {
            return cost.getAmounts().entrySet().stream()
                    .allMatch(entry -> balance.getOrDefault(entry.getKey(), 0) >= entry.getValue());
        }

        @Override
        public void spend(@NotNull SiteKey key, @NotNull ResourceCost cost) {
            cost.getAmounts().forEach((resource, amount) -> balance.merge(resource, -amount, Integer::sum));
        }

        @Override
        public void refund(@NotNull SiteKey key, @NotNull ResourceCost cost) {
            cost.getAmounts().forEach((resource, amount) -> balance.merge(resource, amount, Integer::sum));
        }
    }

    private static final class TestType implements StructureType {

        private final String id;
        private Set<String> required = Set.of();
        private StructureFlags flags = StructureFlags.builder().build();
        private Duration moveTime = Duration.ZERO;
        private ResourceCost moveCost = ResourceCost.NONE;
        private Duration repairTime = Duration.ZERO;
        private ResourceCost repairCost = ResourceCost.NONE;

        private TestType(String id) {
            this.id = id;
        }

        private TestType requires(String type) {
            required = Set.of(type);
            return this;
        }

        private TestType flags(StructureFlags flags) {
            this.flags = flags;
            return this;
        }

        private TestType refund(double share) {
            flags = flags.toBuilder().demolishRefund(share).build();
            return this;
        }

        private TestType move(int minutes, int wood) {
            moveTime = Duration.ofMinutes(minutes);
            moveCost = ResourceCost.of(Map.of("wood", wood));
            return this;
        }

        private TestType repair(int minutes, int wood) {
            repairTime = Duration.ofMinutes(minutes);
            repairCost = ResourceCost.of(Map.of("wood", wood));
            return this;
        }

        @Override
        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.text(id);
        }

        @Override
        public int getTier() {
            return 1;
        }

        @Override
        public @NotNull Set<String> getRequiredStructures() {
            return required;
        }

        @Override
        public String getRequiredZoneTag() {
            return null;
        }

        @Override
        public @NotNull List<StructureStage> getStages() {
            return List.of(
                    new StructureStage(id, ResourceCost.of(Map.of("wood", 10)), Duration.ofMinutes(10)),
                    new StructureStage(id + "_2", ResourceCost.of(Map.of("wood", 20)), Duration.ofMinutes(5)));
        }

        @Override
        public @NotNull StructureFlags getFlags() {
            return flags;
        }

        @Override
        public @NotNull ResourceCost getMoveCost() {
            return moveCost;
        }

        @Override
        public @NotNull Duration getMoveTime() {
            return moveTime;
        }

        @Override
        public @NotNull ResourceCost getRepairCost() {
            return repairCost;
        }

        @Override
        public @NotNull Duration getRepairTime() {
            return repairTime;
        }

        @Override
        public @NotNull List<StructureUpgrade> getUpgrades() {
            final ResourceCost five = ResourceCost.of(Map.of("wood", 5));
            return List.of(
                    new StructureUpgrade("lantern", 0, five, Duration.ZERO, 0, null),
                    new StructureUpgrade("bell", 0, five, Duration.ofMinutes(5), 0, null),
                    new StructureUpgrade("tower", 1, five, Duration.ZERO, 0, null));
        }
    }
}
