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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private ConstructionService service;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> events.add(invocation.getArgument(0))).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);

        world = mock(World.class);
        when(world.getName()).thenReturn("camp_7");

        final SiteInstances instances = mock(SiteInstances.class);
        when(instances.byWorld("camp_7")).thenReturn(Optional.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY)));

        final StructureShapes shapes = mock(StructureShapes.class);
        final SchematicPlacement placement = SchematicPlacement.of(
                new Schematic(1, 1, 1, List.of(new Schematic.PlacedBlock(0, 0, 0, stone()))),
                new Location(world, 0, 64, 0), 0);
        when(shapes.placementOf(any(), anyString(), anyInt(), any())).thenReturn(Optional.of(placement));
        when(shapes.placementOf(any(), any(PlacedStructure.class))).thenReturn(Optional.of(placement));
        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        catalogue.register(new TestType("hall", Set.of(), false, 0.5));
        catalogue.register(new TestType("workshop", Set.of("hall"), false, 0));
        catalogue.register(new TestType("dock", Set.of(), true, 0));
        service = new ConstructionService(instances, catalogue, shapes, fitCheck, now::get);
        service.register("camp", site);
        site.balance.put("wood", 100);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void buildingPaysAndStartsAJob() {
        final ConstructionResult result = build("hall");

        assertTrue(result.isSuccess());
        assertEquals(90, site.balance.get("wood"));
        final PlacedStructure hall = result.getStructure();
        assertEquals(StructureStatus.UNDER_CONSTRUCTION, hall.status(now.get()));
        assertTrue(events.stream().anyMatch(event -> event instanceof StructurePlacedEvent));
        assertTrue(site.changes > 0);
    }

    @Test
    void buildingIsRefusedWithoutPermissionAndNothingIsSpent() {
        site.allowed = false;

        assertFalse(build("hall").isSuccess());
        assertEquals(100, site.balance.get("wood"));
        assertTrue(site.holding.getStructures().isEmpty());
    }

    @Test
    void buildingIsRefusedWithoutEnoughResources() {
        site.balance.put("wood", 5);

        assertFalse(build("hall").isSuccess());
        assertEquals(5, site.balance.get("wood"));
    }

    @Test
    void buildingIsRefusedUntilWhatItNeedsIsFinished() {
        assertFalse(build("workshop").isSuccess());

        final PlacedStructure hall = build("hall").getStructure();
        assertFalse(build("workshop").isSuccess(), "an unfinished hall does not count");

        now.addAndGet(10 * MINUTE);
        service.claim(player, world, hall.getId());
        assertTrue(build("workshop").isSuccess());
    }

    @Test
    void buildingIsRefusedWhereItDoesNotFit() {
        when(fitCheck.problem(any(), any(), any(), any(), any())).thenReturn(Optional.of(Component.text("overlaps")));

        assertFalse(build("hall").isSuccess());
        assertEquals(100, site.balance.get("wood"));
    }

    @Test
    void cancellingAnUnfinishedBuildGivesEverythingBackAndRemovesIt() {
        final PlacedStructure hall = build("hall").getStructure();

        assertTrue(service.cancel(player, world, hall.getId()).isSuccess());

        assertEquals(100, site.balance.get("wood"));
        assertTrue(site.holding.getStructures().isEmpty());
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureRemovedEvent removed
                && !removed.isDemolished()));
    }

    @Test
    void aJobCanOnlyBeClaimedOnceItIsDone() {
        final PlacedStructure hall = build("hall").getStructure();
        assertFalse(service.claim(player, world, hall.getId()).isSuccess());

        now.addAndGet(10 * MINUTE);
        assertTrue(service.claim(player, world, hall.getId()).isSuccess());

        assertEquals(StructureCondition.ACTIVE, hall.getCondition());
        assertNull(hall.getJob());
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureStatusChangeEvent change
                && change.getTo() == StructureStatus.ACTIVE));
    }

    @Test
    void somethingThatStartsBrokenNeedsARepairOnceBuilt() {
        final PlacedStructure dock = build("dock").getStructure();
        now.addAndGet(10 * MINUTE);

        service.claim(player, world, dock.getId());

        assertEquals(StructureCondition.NEEDS_REPAIR, dock.getCondition());
        assertTrue(service.repair(player, world, dock.getId()).isSuccess());
        assertEquals(StructureCondition.ACTIVE, dock.getCondition(), "a repair with no time set is instant");
    }

    @Test
    void advancingRaisesTheStageOnceClaimed() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.advance(player, world, hall.getId()).isSuccess());
        assertEquals(0, hall.getStage(), "not until it is claimed");
        assertEquals(StructureStatus.ADVANCING, hall.status(now.get()));

        now.addAndGet(5 * MINUTE);
        service.claim(player, world, hall.getId());
        assertEquals(1, hall.getStage());
        assertFalse(service.advance(player, world, hall.getId()).isSuccess(), "there is no stage after that");
    }

    @Test
    void demolishingGivesBackItsShareAndDropsWhatItHeldOnceItIsGone() {
        final PlacedStructure hall = finished("hall");
        assertEquals(90, site.balance.get("wood"));

        assertTrue(service.demolish(player, world, hall.getId()).isSuccess());

        assertEquals(95, site.balance.get("wood"), "half of the 10 it cost");
        assertEquals(1, site.dropsSeenWithoutTheStructure,
                "contents drop after the structure has left the holding");
    }

    @Test
    void aStructureWithAJobRunningCannotBeDemolished() {
        final PlacedStructure hall = finished("hall");
        service.advance(player, world, hall.getId());

        assertFalse(service.demolish(player, world, hall.getId()).isSuccess());
    }

    @Test
    void anInstantMoveTakesEffectStraightAway() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.move(player, world, hall.getId(), new Location(world, 30, 64, 5), 2).isSuccess());

        assertEquals(new StructurePosition(30, 64, 5, 2), hall.getPosition());
        assertNull(hall.getJob());
    }

    @Test
    void aJobRuleHoldsWorkAndLetsItGoAgain() {
        final boolean[] siege = {false};
        site.rules.add(new JobRule() {
            @Override
            public @NotNull String id() {
                return "siege";
            }

            @Override
            public boolean holds(@NotNull SiteKey key, @NotNull PlacedStructure structure, @NotNull Job job) {
                return siege[0];
            }
        });
        final PlacedStructure hall = build("hall").getStructure();
        final ConstructionService.Worksite worksite = service.worksite(world).orElseThrow();

        siege[0] = true;
        service.refresh(worksite);
        assertEquals(StructureStatus.PAUSED, hall.status(now.get()));

        now.addAndGet(60 * MINUTE);
        siege[0] = false;
        service.refresh(worksite);
        assertEquals(StructureStatus.UNDER_CONSTRUCTION, hall.status(now.get()), "time under siege did not count");
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureStatusChangeEvent change
                && change.getTo() == StructureStatus.PAUSED));
    }

    @Test
    void aFinishedJobIsNeverHeldAgain() {
        final boolean[] siege = {false};
        site.rules.add(new JobRule() {
            @Override
            public @NotNull String id() {
                return "siege";
            }

            @Override
            public boolean holds(@NotNull SiteKey key, @NotNull PlacedStructure structure, @NotNull Job job) {
                return siege[0];
            }
        });
        final PlacedStructure hall = build("hall").getStructure();
        final ConstructionService.Worksite worksite = service.worksite(world).orElseThrow();

        now.addAndGet(60 * MINUTE);
        siege[0] = true;
        service.refresh(worksite);
        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()), "its crew leaving must not stop the claim");
    }

    @Test
    void availabilityCanBeAskedWithoutTheWorld() {
        final StructureType workshop = catalogue.find("workshop").orElseThrow();
        assertTrue(service.unavailable(player, CAMP, workshop).isPresent(), "needs a hall first");

        finished("hall");
        assertTrue(service.unavailable(player, CAMP, workshop).isEmpty());

        site.balance.put("wood", 0);
        assertTrue(service.unavailable(player, CAMP, workshop).isPresent(), "cannot afford it");
    }

    @Test
    void anInstantUpgradeIsFittedAtOnceAndTakesItsStagesPick() {
        final PlacedStructure hall = finished("hall");

        assertTrue(service.upgrade(player, world, hall.getId(), "lantern").isSuccess());

        assertTrue(hall.hasUpgrade("lantern"));
        assertEquals(85, site.balance.get("wood"));
        assertTrue(events.stream().anyMatch(event -> event instanceof StructureUpgradedEvent upgraded
                && upgraded.getUpgrade().getId().equals("lantern")));
        assertFalse(service.upgrade(player, world, hall.getId(), "bell").isSuccess(), "one pick per stage");
        assertEquals(85, site.balance.get("wood"));
    }

    @Test
    void aTimedUpgradeIsFittedOnlyOnceClaimed() {
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
    void cancellingAnUpgradeGivesItsCostBackAndLeavesTheStageOpen() {
        final PlacedStructure hall = finished("hall");
        service.upgrade(player, world, hall.getId(), "bell");

        assertTrue(service.cancel(player, world, hall.getId()).isSuccess());

        assertEquals(90, site.balance.get("wood"));
        assertTrue(hall.upgradeAt(0).isEmpty());
        assertTrue(service.upgrade(player, world, hall.getId(), "lantern").isSuccess());
    }

    @Test
    void aLaterStagesUpgradeWaitsForItAndAnEarlierStageCanStillBePicked() {
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
    void upgradeAvailabilityCanBeAskedWithoutTheWorld() {
        final PlacedStructure hall = finished("hall");
        assertTrue(service.upgradeUnavailable(player, CAMP, hall.getId(), "lantern").isEmpty());

        service.upgrade(player, world, hall.getId(), "lantern");
        assertTrue(service.upgradeUnavailable(player, CAMP, hall.getId(), "bell").isPresent());
    }

    private ConstructionResult build(String type) {
        return service.build(player, world, catalogue.find(type).orElseThrow(), new Location(world, 0, 64, 0), 0);
    }

    private PlacedStructure finished(String type) {
        final PlacedStructure structure = build(type).getStructure();
        now.addAndGet(10 * MINUTE);
        service.claim(player, world, structure.getId());
        return structure;
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
        private boolean allowed = true;
        private int changes;
        private int dropsSeenWithoutTheStructure;

        @Override
        public @NotNull Optional<Holding> holding(@NotNull SiteKey key) {
            return Optional.of(holding);
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
            return allowed;
        }

        @Override
        public @NotNull List<JobRule> jobRules() {
            return rules;
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
        private final Set<String> required;
        private final StructureFlags flags;

        private TestType(String id, Set<String> required, boolean startsBroken, double demolishRefund) {
            this.id = id;
            this.required = required;
            this.flags = StructureFlags.builder().startsBroken(startsBroken).demolishRefund(demolishRefund).build();
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
        public @NotNull List<StructureUpgrade> getUpgrades() {
            final ResourceCost five = ResourceCost.of(Map.of("wood", 5));
            return List.of(
                    new StructureUpgrade("lantern", 0, five, Duration.ZERO, 0, null),
                    new StructureUpgrade("bell", 0, five, Duration.ofMinutes(5), 0, null),
                    new StructureUpgrade("tower", 1, five, Duration.ZERO, 0, null));
        }
    }
}
