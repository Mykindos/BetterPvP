package me.mykindos.betterpvp.core.world.construction.view;

import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.construction.ComponentKeys;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.construction.ConstructionSite;
import me.mykindos.betterpvp.core.world.construction.Worksite;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import me.mykindos.betterpvp.core.world.construction.TestStructureType;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.schematic.BlockBatchStore;
import me.mykindos.betterpvp.core.world.schematic.LayerPlan;
import me.mykindos.betterpvp.core.world.schematic.RenderedBuild;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicRenderer;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The hall is five blocks set corner to corner, one per layer, rising from its anchor at (100, 64, 100): stone at
 * (100, 64, 100), a chest at (102, 65, 100), then stone at (104, 66), (106, 67) and (108, 68). Its lantern upgrade's
 * piece is one block at (104, 69, 100). The store is two chests side by side at (200, 64, 200) and (201, 64, 200).
 * Every build job takes ten minutes.
 */
class StructureViewsTest {

    private static final long MINUTE = 60_000;
    private static final SiteKey CAMP = SiteKey.of("camp", 7);

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final ConstructionService service = mock(ConstructionService.class);
    private final ConstructionSites sites = mock(ConstructionSites.class);
    private final StructureStatusTracker tracker = mock(StructureStatusTracker.class);
    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final StructureShapes shapes = mock(StructureShapes.class);
    private final ConstructionSite site = mock(ConstructionSite.class);
    private final Holding holding = new Holding();
    private final World world = mock(World.class);
    private final WorldContentScope scope = mock(WorldContentScope.class);
    private final Player player = mock(Player.class);
    private final SceneObjectRegistry registry = mock(SceneObjectRegistry.class);
    private final List<SceneSpawn> spawns = new ArrayList<>();
    private final List<Interaction> hitboxParts = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final Map<UUID, RenderedBuild> builds = new HashMap<>();
    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<String, Inventory> inventories = new HashMap<>();
    private final Schematic hallSchematic = new Schematic(9, 5, 1, List.of(
            new Schematic.PlacedBlock(0, 0, 0, data(Material.STONE)),
            new Schematic.PlacedBlock(2, 1, 0, data(Material.CHEST)),
            new Schematic.PlacedBlock(4, 2, 0, data(Material.STONE)),
            new Schematic.PlacedBlock(6, 3, 0, data(Material.STONE)),
            new Schematic.PlacedBlock(8, 4, 0, data(Material.STONE))));
    private final Schematic storeSchematic = new Schematic(2, 1, 1, List.of(
            new Schematic.PlacedBlock(0, 0, 0, data(Material.CHEST)),
            new Schematic.PlacedBlock(1, 0, 0, data(Material.CHEST))));
    private final Schematic lanternSchematic = new Schematic(1, 1, 1, List.of(
            new Schematic.PlacedBlock(0, 0, 0, data(Material.STONE))));

    private MockedStatic<Bukkit> bukkit;
    private boolean handleUse;
    private StructureViews views;

    @BeforeEach
    void setUp() {
        final PluginManager plugins = mock(PluginManager.class);
        doAnswer(invocation -> {
            final Event event = invocation.getArgument(0);
            events.add(event);
            if (handleUse && event instanceof StructurePieceUseEvent use) {
                use.setHandled(true);
            }
            return null;
        }).when(plugins).callEvent(any());
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(() -> Bukkit.getWorld("camp_7")).thenReturn(world);

        when(world.getName()).thenReturn("camp_7");
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        doReturn(mock(BlockDisplay.class)).when(world).spawn(any(Location.class), eq(BlockDisplay.class),
                any(Consumer.class));
        doAnswer(invocation -> {
            final Interaction part = mock(Interaction.class);
            final Location at = invocation.getArgument(0);
            when(part.getLocation()).thenReturn(at);
            when(part.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            invocation.<Consumer<Interaction>>getArgument(2).accept(part);
            hitboxParts.add(part);
            return part;
        }).when(world).spawn(any(Location.class), eq(Interaction.class), any(Consumer.class));

        catalogue.register(new TestStructureType("hall", new StructureUpgrade("lantern", 0, ResourceCost.NONE,
                Duration.ZERO, 0, "lantern_piece")));
        catalogue.register(new TestStructureType("store"));

        when(shapes.placementOf(any(), any(PlacedStructure.class))).thenAnswer(invocation -> {
            final PlacedStructure structure = invocation.getArgument(1);
            final Schematic schematic = structure.getType().equals("store") ? storeSchematic : hallSchematic;
            return Optional.of(SchematicPlacement.of(schematic, structure.getPosition().toLocation(world),
                    structure.getPosition().getQuarterTurns()));
        });
        when(shapes.pieceOf(any(), any(), any())).thenAnswer(invocation -> Optional.of(
                SchematicPlacement.of(lanternSchematic, new Location(world, 104, 69, 100), 0)));

        final SchematicRenderer renderer = spy(new SchematicRenderer(mock(BlockBatchStore.class)));
        doReturn(List.of()).when(renderer).paste(any(SchematicPlacement.class), anyCollection());
        doNothing().when(renderer).restore(any(), anyList());
        doAnswer(invocation -> {
            final RenderedBuild build = (RenderedBuild) invocation.callRealMethod();
            builds.put(build.getId(), build);
            return build;
        }).when(renderer).open(any(), any(), any());

        when(tracker.now()).thenAnswer(invocation -> now.get());
        when(sites.worksite(world)).thenReturn(Optional.of(
                new Worksite(CAMP, site, holding, world)));
        when(sites.canUse(any(), any(), any())).thenReturn(true);
        doAnswer(invocation -> spawns.add(invocation.getArgument(0))).when(scope).add(any(SceneSpawn.class));

        views = new StructureViews(service, sites, tracker, catalogue, mock(SiteInstances.class), shapes, renderer,
                registry, mock(ConstructionPropFactory.class));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void ac11_whileABuildRunsTheLayersStandingAreItsProgressTimesTheLayerCountRoundedDown() {
        final PlacedStructure hall = building("hall");
        now.addAndGet(4 * MINUTE);
        load();
        assertEquals(2, builds.get(hall.getId()).getShown(), "0.4 of 5 layers");

        now.addAndGet(MINUTE + MINUTE / 2);
        views.tick();
        assertEquals(2, builds.get(hall.getId()).getShown(), "0.55 of 5 layers rounds down");

        now.addAndGet(MINUTE / 2);
        views.tick();
        assertEquals(3, builds.get(hall.getId()).getShown(), "0.6 of 5 layers");
    }

    @Test
    void ac11_theBuildIsWholeWhenItsTimeIsUp() {
        final PlacedStructure hall = building("hall");
        now.addAndGet(10 * MINUTE - 1);
        load();
        assertEquals(4, builds.get(hall.getId()).getShown());

        now.addAndGet(1);
        views.tick();
        assertEquals(5, builds.get(hall.getId()).getShown());
    }

    @Test
    void ac11_anyOtherJobLeavesEveryLayerStanding() {
        final PlacedStructure advancing = withJob(active("hall"), JobKind.ADVANCE);
        final PlacedStructure moving = withJob(active("hall"), JobKind.MOVE);
        final PlacedStructure repairing = withJob(placed("hall", StructureCondition.NEEDS_REPAIR), JobKind.REPAIR);
        final PlacedStructure fitting = withJob(active("hall"), JobKind.FIT_UPGRADE);
        final PlacedStructure idle = active("hall");
        now.addAndGet(MINUTE);

        load();

        for (PlacedStructure structure : List.of(advancing, moving, repairing, fitting, idle)) {
            assertEquals(5, builds.get(structure.getId()).getShown(), structure.getJob() == null ? "no job"
                    : structure.getJob().getKind().name());
        }
    }

    @Test
    void ac13_theLabelStandsOnTheBuildsLabelPoint() {
        final Location point = new Location(world, 101.5, 66.0, 99.5);
        when(shapes.point(any(), any(), eq("label"))).thenReturn(Optional.of(point));
        building("hall");

        load();

        assertEquals(point, spawns.getFirst().getAnchor());
    }

    @Test
    void ac13_withoutALabelPointTheLabelStandsAboveTheRoof() {
        building("hall");

        load();

        final Location at = spawns.getFirst().getAnchor();
        assertEquals(104.5, at.getX(), 1e-9);
        assertEquals(100.5, at.getZ(), 1e-9);
        assertEquals(69.5, at.getY(), 1e-9, "half a block above the top of the build at y 69");
    }

    @Test
    void ac13_eachStructureShowsItsLabel() {
        building("hall");
        now.addAndGet(MINUTE);

        load();

        final Component label = shownText(materialize(0));
        assertTrue(ComponentKeys.text(label).startsWith("Hall"));
        assertTrue(ComponentKeys.hasKey(label, "core.construction.label.building"));
    }

    @Test
    void ac13_theLabelNamesTheStructureThenSaysWhatIsHappeningWithTheTimeLeft() {
        final StructureType hall = catalogue.find("hall").orElseThrow();
        assertTimed(hall, building("hall"), "building");
        assertTimed(hall, withJob(active("hall"), JobKind.MOVE), "moving");
        assertTimed(hall, withJob(active("hall"), JobKind.ADVANCE), "advancing");
        assertTimed(hall, withJob(placed("hall", StructureCondition.NEEDS_REPAIR), JobKind.REPAIR), "repairing");
        assertTimed(hall, withJob(placed("hall", StructureCondition.DISABLED), JobKind.REPAIR), "repairing");
        assertTimed(hall, withJob(active("hall"), JobKind.FIT_UPGRADE), "upgrading");
    }

    @Test
    void ac13_aPausedLabelShowsTheTimeLeftFrozen() {
        final StructureType hall = catalogue.find("hall").orElseThrow();
        final PlacedStructure structure = building("hall");
        now.addAndGet(3 * MINUTE);
        structure.getJob().hold("siege", now.get());
        final String left = UtilTime.humanReadableFormat(Duration.ofMillis(7 * MINUTE));

        final Component label = StructureView.label(hall, structure, StructureStatus.PAUSED, now.get());
        now.addAndGet(2 * MINUTE);
        final Component later = StructureView.label(hall, structure, StructureStatus.PAUSED, now.get());

        final TranslatableComponent paused = ComponentKeys.find(label, "core.construction.label.paused").orElseThrow();
        assertTrue(ComponentKeys.text(paused).contains(left), "paused label says " + ComponentKeys.text(paused));
        assertEquals(label, later);
    }

    @Test
    void ac13_aFinishedJobSaysReadyAndABrokenStructureWithNoJobSaysSo() {
        final StructureType hall = catalogue.find("hall").orElseThrow();
        final PlacedStructure ready = building("hall");
        now.addAndGet(10 * MINUTE);

        assertLine(hall, ready, StructureStatus.READY_TO_CLAIM, "core.construction.label.ready");
        assertLine(hall, placed("hall", StructureCondition.DISABLED), StructureStatus.DISABLED,
                "core.construction.label.disabled");
        assertLine(hall, placed("hall", StructureCondition.NEEDS_REPAIR), StructureStatus.NEEDS_REPAIR,
                "core.construction.label.needs_repair");
    }

    @Test
    void ac13_aStandingStructureWithNoJobAndNothingToReportShowsNoLabel() {
        final StructureType hall = catalogue.find("hall").orElseThrow();
        final PlacedStructure idle = active("hall");

        assertEquals(Component.empty(), StructureView.label(hall, idle, StructureStatus.ACTIVE, now.get()));
        load();
        assertEquals(Component.empty(), shownText(materialize(0)));
    }

    @Test
    void ac16_clickingTheLabelOfAWaitingStructureClaimsItForThatPlayer() {
        final PlacedStructure hall = ready("hall");
        when(service.claim(any(), any(), any())).thenReturn(ConstructionResult.done(hall));
        load();
        materialize(0);

        assertEquals(1, hitboxParts.size());
        prop(0).act(player);

        verify(service).claim(player, world, hall.getId());
    }

    @Test
    void ac16_aRefusedClaimTellsThePlayerWhy() {
        ready("hall");
        when(service.claim(any(), any(), any())).thenReturn(ConstructionResult.refused(Component.text("Not yours")));
        load();

        prop(0).act(player);

        final ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(message.capture());
        assertEquals("Not yours", ComponentKeys.text(message.getValue()));
    }

    @Test
    void ac16_onlyTheLabelCarriesTheClaimHitbox() {
        ready("hall");
        load();
        materialize(0);

        final BoundingBox area = hitbox();
        assertTrue(area.contains(spawns.getFirst().getAnchor().toVector()), "the hitbox is on the label");
        assertFalse(area.contains(100.5, 64.5, 100.5), "the bottom block of the build does not claim");
        assertFalse(area.contains(108.5, 68.5, 100.5), "the top corner of the build does not claim");
    }

    @Test
    void ac16_onceClaimedTheHitboxIsGone() {
        final PlacedStructure hall = ready("hall");
        load();
        materialize(0);
        final Interaction part = hitboxParts.getFirst();

        hall.setJob(null);
        hall.setCondition(StructureCondition.ACTIVE);
        views.tick();
        prop(0).act(player);

        verify(part).remove();
        verify(registry).unalias(part);
        verify(service, never()).claim(any(), any(), any());
    }

    @Test
    void ac17_nothingInAStructureUnderConstructionCanBeUsed() {
        building("hall");
        now.addAndGet(5 * MINUTE);
        load();

        final PlayerInteractEvent openChest = rightClick(102, 65, 100);
        views.onUse(openChest);

        verify(openChest).setCancelled(true);
    }

    @Test
    void ac17_nothingInAStructureWaitingToBeClaimedCanBeUsed() {
        final PlacedStructure hall = withJob(active("hall"), JobKind.ADVANCE);
        hall.getUpgrades().put(0, "lantern");
        now.addAndGet(10 * MINUTE);
        load();
        assertEquals(StructureStatus.READY_TO_CLAIM, hall.status(now.get()));

        final PlayerInteractEvent openChest = rightClick(102, 65, 100);
        views.onUse(openChest);
        final PlayerInteractEvent usePiece = rightClick(104, 69, 100);
        views.onUse(usePiece);

        verify(openChest).setCancelled(true);
        verify(usePiece).setCancelled(true);
        assertTrue(events.stream().noneMatch(StructurePieceUseEvent.class::isInstance));
    }

    @Test
    void ac17_aPausedFirstBuildStaysLocked() {
        final PlacedStructure hall = building("hall");
        now.addAndGet(5 * MINUTE);
        hall.getJob().hold("siege", now.get());
        load();
        assertEquals(StructureStatus.PAUSED, hall.status(now.get()));

        final PlayerInteractEvent openChest = rightClick(102, 65, 100);
        views.onUse(openChest);

        verify(openChest).setCancelled(true);
    }

    @Test
    void ac17_anActiveStructuresContainersStillOpen() {
        active("hall");
        load();

        final PlayerInteractEvent openChest = rightClick(102, 65, 100);
        views.onUse(openChest);

        verify(openChest, never()).setCancelled(true);
    }

    @Test
    void ac18_clickingAPieceOfAnActiveStructureFiresTheUseEvent() {
        final PlacedStructure hall = active("hall");
        hall.getUpgrades().put(0, "lantern");
        handleUse = true;
        load();

        final PlayerInteractEvent click = rightClick(104, 69, 100);
        views.onUse(click);

        final StructurePieceUseEvent use = events.stream().filter(StructurePieceUseEvent.class::isInstance)
                .map(StructurePieceUseEvent.class::cast).findFirst().orElseThrow();
        assertSame(player, use.getPlayer());
        assertEquals(CAMP, use.getSite());
        assertSame(hall, use.getStructure());
        assertEquals("lantern", use.getUpgrade().getId());
        verify(click).setCancelled(true);
    }

    @Test
    void ac18_aUseNobodyHandlesLeavesTheClickAlone() {
        active("hall").getUpgrades().put(0, "lantern");
        load();

        final PlayerInteractEvent click = rightClick(104, 69, 100);
        views.onUse(click);

        assertEquals(1, events.stream().filter(StructurePieceUseEvent.class::isInstance).count());
        verify(click, never()).setCancelled(true);
    }

    @Test
    void ac18_onAStructureThatIsNotActiveTheClickIsStoppedAndNothingFires() {
        placed("hall", StructureCondition.DISABLED).getUpgrades().put(0, "lantern");
        handleUse = true;
        load();

        final PlayerInteractEvent click = rightClick(104, 69, 100);
        views.onUse(click);

        verify(click).setCancelled(true);
        assertTrue(events.stream().noneMatch(StructurePieceUseEvent.class::isInstance));
    }

    @Test
    void ac18_aPlayerWhoMayNotUseItIsToldItIsNotTheirsAndNothingFires() {
        active("hall").getUpgrades().put(0, "lantern");
        when(sites.canUse(any(), any(), any())).thenReturn(false);
        load();

        final PlayerInteractEvent click = rightClick(104, 69, 100);
        views.onUse(click);

        verify(click).setCancelled(true);
        assertTrue(events.stream().noneMatch(StructurePieceUseEvent.class::isInstance));
        final ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(message.capture());
        assertTrue(ComponentKeys.hasKey(message.getValue(), "core.construction.not_yours"));
    }

    @Test
    void ac19_aLabelThatOnlyInformsHasNoHitbox() {
        building("hall");
        withJob(active("hall"), JobKind.ADVANCE);
        building("hall").getJob().hold("siege", now.get());
        placed("hall", StructureCondition.DISABLED);
        placed("hall", StructureCondition.NEEDS_REPAIR);
        withJob(placed("hall", StructureCondition.NEEDS_REPAIR), JobKind.REPAIR);
        now.addAndGet(MINUTE);

        load();

        for (int i = 0; i < holding.getStructures().size(); i++) {
            materialize(i);
        }

        assertTrue(hitboxParts.isEmpty(), "no label here expects an action, so none has a hitbox");
    }

    @Test
    void ac19_aLabelThatExpectsAnActionHasAHitboxOnTheLabel() {
        ready("hall");
        load();
        materialize(0);

        final BoundingBox area = hitbox();
        assertTrue(area.contains(spawns.getFirst().getAnchor().toVector()));
        assertTrue(area.getVolume() < 8, "sized to the label, not the building, was " + area);
    }

    @Test
    void ac22_closingAStructuresContainerWritesItsContentsDownAndAsksTheSiteToSave() {
        final PlacedStructure hall = active("hall");
        load();
        fill(102, 65, 100);

        views.onClose(closing((Chest) block(102, 65, 100).getState(false)));

        assertNotNull(hall.getStorage());
        assertEquals(Set.of("2,1,0"), hall.getStorage().keySet());
        verify(site, atLeastOnce()).changed(CAMP);
    }

    @Test
    void ac22_bothHalvesOfADoubleChestAreWrittenDown() {
        final PlacedStructure store = placed("store", StructureCondition.ACTIVE);
        store.setPosition(new StructurePosition(200, 64, 200, 0));
        load();
        fill(200, 64, 200);
        fill(201, 64, 200);
        final Chest left = (Chest) block(200, 64, 200).getState(false);
        final Chest right = (Chest) block(201, 64, 200).getState(false);
        final DoubleChest chest = mock(DoubleChest.class);
        when(chest.getLeftSide(false)).thenReturn(left);
        when(chest.getRightSide(false)).thenReturn(right);

        views.onClose(closing(chest));

        assertNotNull(store.getStorage());
        assertEquals(Set.of("0,0,0", "1,0,0"), store.getStorage().keySet());
        verify(site, atLeastOnce()).changed(CAMP);
    }

    private void load() {
        views.content().install(world, mock(RegionIndex.class), scope);
    }

    private @NotNull StructureProp prop(int index) {
        return (StructureProp) spawns.get(index).getObject();
    }

    /** Gives a label its body, as its chunk loading would, and returns that body. */
    private @NotNull TextDisplay materialize(int index) {
        final TextDisplay display = mock(TextDisplay.class);
        when(display.getWorld()).thenReturn(world);
        when(display.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        prop(index).init(display);
        return display;
    }

    private static @NotNull Component shownText(@NotNull TextDisplay display) {
        final ArgumentCaptor<Component> text = ArgumentCaptor.forClass(Component.class);
        verify(display).text(text.capture());
        return text.getValue();
    }

    /** The box covered by the one interaction entity the prop spawned to be clicked. */
    private @NotNull BoundingBox hitbox() {
        assertEquals(1, hitboxParts.size(), "one hitbox part");
        final Interaction part = hitboxParts.getFirst();
        final ArgumentCaptor<Float> width = ArgumentCaptor.forClass(Float.class);
        final ArgumentCaptor<Float> height = ArgumentCaptor.forClass(Float.class);
        verify(part).setInteractionWidth(width.capture());
        verify(part).setInteractionHeight(height.capture());
        final Location at = part.getLocation();
        final double half = width.getValue() / 2;
        return new BoundingBox(at.getX() - half, at.getY(), at.getZ() - half,
                at.getX() + half, at.getY() + height.getValue(), at.getZ() + half);
    }

    private void assertTimed(@NotNull StructureType type, @NotNull PlacedStructure structure, @NotNull String doing) {
        final long at = now.get() + MINUTE;
        final Component label = StructureView.label(type, structure, structure.status(at), at);
        final String left = UtilTime.humanReadableFormat(Duration.ofMillis(structure.getJob().remainingMillis(at)));
        assertTrue(ComponentKeys.text(label).startsWith("Hall"), "first line is the name");
        final TranslatableComponent line = ComponentKeys.find(label, "core.construction.label." + doing)
                .orElseThrow(() -> new AssertionError("no " + doing + " line in " + label));
        assertEquals(left, ComponentKeys.text(line));
    }

    private static void assertLine(@NotNull StructureType type, @NotNull PlacedStructure structure,
                                   @NotNull StructureStatus status, @NotNull String key) {
        final Component label = StructureView.label(type, structure, status, 0);
        assertTrue(ComponentKeys.text(label).startsWith("Hall"));
        assertTrue(ComponentKeys.hasKey(label, key), key);
    }

    private @NotNull PlacedStructure placed(@NotNull String type, @NotNull StructureCondition condition) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type,
                new StructurePosition(100, 64, 100, 0), condition);
        holding.getStructures().add(structure);
        return structure;
    }

    private @NotNull PlacedStructure active(@NotNull String type) {
        return placed(type, StructureCondition.ACTIVE);
    }

    private @NotNull PlacedStructure building(@NotNull String type) {
        final PlacedStructure structure = placed(type, StructureCondition.UNDER_CONSTRUCTION);
        structure.setJob(Job.start(JobKind.BUILD, Duration.ofMinutes(10), ResourceCost.NONE, 0, now.get()));
        return structure;
    }

    private @NotNull PlacedStructure ready(@NotNull String type) {
        final PlacedStructure structure = building(type);
        structure.getJob().finish(now.get());
        return structure;
    }

    private @NotNull PlacedStructure withJob(@NotNull PlacedStructure structure, @NotNull JobKind kind) {
        structure.setJob(Job.start(kind, Duration.ofMinutes(10), ResourceCost.NONE, structure.getStage() + 1,
                now.get()));
        return structure;
    }

    private @NotNull PlayerInteractEvent rightClick(int x, int y, int z) {
        final Block clicked = block(x, y, z);
        final PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(clicked);
        return event;
    }

    private @NotNull InventoryCloseEvent closing(@NotNull InventoryHolder holder) {
        final Inventory open = mock(Inventory.class);
        when(open.getHolder(false)).thenReturn(holder);
        final InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getInventory()).thenReturn(open);
        return event;
    }

    private void fill(int x, int y, int z) {
        final ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND);
        when(item.serializeAsBytes()).thenReturn(new byte[]{1, 2, 3});
        final Inventory inventory = inventories.get(x + "," + y + "," + z);
        when(inventory.getContents()).thenReturn(new ItemStack[]{item, null});
    }

    /** The world's block at a position. Chests in either build stand where the builds put them. */
    private @NotNull Block block(int x, int y, int z) {
        return blocks.computeIfAbsent(x + "," + y + "," + z, key -> {
            final Block block = mock(Block.class);
            when(block.getWorld()).thenReturn(world);
            when(block.getX()).thenReturn(x);
            when(block.getY()).thenReturn(y);
            when(block.getZ()).thenReturn(z);
            final boolean chest = (x == 102 && y == 65 && z == 100) || (y == 64 && z == 200 && (x == 200 || x == 201));
            when(block.getType()).thenReturn(chest ? Material.CHEST : Material.STONE);
            if (chest) {
                final Inventory inventory = mock(Inventory.class);
                when(inventory.getSize()).thenReturn(2);
                when(inventory.getContents()).thenReturn(new ItemStack[2]);
                inventories.put(key, inventory);
                final Chest state = mock(Chest.class);
                when(state.getWorld()).thenReturn(world);
                when(state.getX()).thenReturn(x);
                when(state.getY()).thenReturn(y);
                when(state.getZ()).thenReturn(z);
                when(state.getBlockInventory()).thenReturn(inventory);
                when(block.getState(false)).thenReturn(state);
            }
            return block;
        });
    }

    private static @NotNull BlockData data(@NotNull Material material) {
        final BlockData data = mock(BlockData.class);
        when(data.getMaterial()).thenReturn(material);
        return data;
    }
}
