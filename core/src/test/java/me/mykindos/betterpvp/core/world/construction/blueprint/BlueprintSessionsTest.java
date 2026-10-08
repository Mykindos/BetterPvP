package me.mykindos.betterpvp.core.world.construction.blueprint;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleQueue;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import me.mykindos.betterpvp.core.world.construction.ComponentKeys;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionChecks;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.ConstructionSite;
import me.mykindos.betterpvp.core.world.construction.Worksite;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.TestStructureType;
import me.mykindos.betterpvp.core.world.schematic.BlockTransform;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreview;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreviews;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The player looks at a block whose top is at (10, 64, 10), so a preview anchors at (10, 65, 10). Every build is
 * captured facing yaw 0, and the player faces yaw 90 unless a test turns them.
 */
class BlueprintSessionsTest {

    private static final SiteKey CAMP = SiteKey.of("camp", 7);

    private final StructureBlueprintItem blueprintItem = mock(StructureBlueprintItem.class);
    private final ItemFactory itemFactory = mock(ItemFactory.class);
    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final SchematicService schematics = mock(SchematicService.class);
    private final ConstructionService construction = mock(ConstructionService.class);
    private final ConstructionSites sites = mock(ConstructionSites.class);
    private final ConstructionChecks checks = mock(ConstructionChecks.class);
    private final GhostPreviews previews = mock(GhostPreviews.class);
    private final ClientManager clients = mock(ClientManager.class);
    private final Gamer gamer = mock(Gamer.class);
    private final TitleQueue titles = mock(TitleQueue.class);
    private final ActionBar actionBar = mock(ActionBar.class);
    private final Player player = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);
    private final World world = mock(World.class);
    private final Block target = mock(Block.class);
    private final Holding holding = new Holding();
    private final List<GhostPreview> opened = new ArrayList<>();
    private final List<Schematic> openedFor = new ArrayList<>();

    private MockedStatic<Bukkit> bukkit;
    private Location anchor;
    private BlueprintSessions sessions;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(player));

        catalogue.register(new TestStructureType("hall"));
        catalogue.register(new TestStructureType("workshop"));
        for (StructureType type : catalogue.all()) {
            for (int stage = 0; stage < type.getStages().size(); stage++) {
                final String name = type.stage(stage).getSchematic();
                final Schematic schematic = new Schematic(1, 1, 1, 0, 0, 0, List.of(), List.of(), 0f);
                when(schematics.load(name)).thenReturn(Optional.of(schematic));
            }
        }
        when(previews.open(any(), any())).thenAnswer(invocation -> {
            final GhostPreview preview = mock(GhostPreview.class);
            opened.add(preview);
            openedFor.add(invocation.getArgument(1));
            return preview;
        });

        final SearchEngineBase<Client> search = mock(SearchEngineBase.class);
        final Client client = mock(Client.class);
        when(clients.search()).thenReturn(search);
        when(search.online(player)).thenReturn(client);
        when(client.getGamer()).thenReturn(gamer);
        when(gamer.getTitleQueue()).thenReturn(titles);
        when(gamer.getActionBar()).thenReturn(actionBar);

        when(world.getName()).thenReturn("camp_7");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.getInventory()).thenReturn(inventory);
        face(90);
        anchor = new Location(world, 10, 65, 10);
        final Block above = mock(Block.class);
        when(above.getLocation()).thenReturn(anchor);
        when(target.getRelative(BlockFace.UP)).thenReturn(above);
        when(player.getTargetBlockExact(anyInt())).thenReturn(target);

        when(checks.problem(any(), any(), any(), any(), anyInt())).thenReturn(Optional.empty());
        when(checks.moveProblem(any(), any(), any(), any(), anyInt())).thenReturn(Optional.empty());
        when(sites.worksite(world)).thenReturn(Optional.of(
                new Worksite(CAMP, mock(ConstructionSite.class), holding, world)));

        sessions = new BlueprintSessions(blueprintItem, itemFactory, catalogue, schematics, construction, sites, checks, previews,
                clients);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void ac2_onlyTheBlueprintItemWithACataloguedTypeIsABlueprint() {
        assertEquals("hall", sessions.structureOf(blueprint("hall", null)).orElseThrow().getId());
        assertTrue(sessions.structureOf(blueprint("ruin", null)).isEmpty(), "type not in the catalogue");
        assertTrue(sessions.structureOf(otherItem()).isEmpty(), "not the blueprint item");
        assertTrue(sessions.structureOf(air()).isEmpty());
        assertTrue(sessions.structureOf(null).isEmpty());
    }

    @Test
    void ac2_everyHandlerIgnoresAnythingThatIsNotABlueprint() {
        for (ItemStack held : List.of(blueprint("ruin", null), otherItem(), air())) {
            hold(held);
            sessions.follow();
            sneak(true);
            final PlayerInteractEvent click = click(Action.RIGHT_CLICK_BLOCK);
            sessions.onUse(click);
            verify(click, never()).setCancelled(true);
        }

        verify(previews, never()).open(any(), any());
        verify(construction, never()).build(any(), any(), any(), any(), anyInt());
        verify(construction, never()).move(any(), any(), any(), any(), anyInt());
    }

    @Test
    void ac4_holdingABuildBlueprintOpensAPreviewOfStageZero() {
        hold(blueprint("hall", null));

        sessions.follow();

        verify(previews).open(eq(player), any());
        assertEquals(schematic("hall_0"), openedFor.getFirst());
    }

    @Test
    void ac4_holdingAMoveBlueprintOpensAPreviewOfTheStageTheStructureIsAt() {
        final PlacedStructure hall = placed("hall", 2);
        hold(blueprint("hall", hall.getId()));

        sessions.follow();

        assertEquals(schematic("hall_2"), openedFor.getFirst());
    }

    @Test
    void ac4_puttingItAwayClosesThePreviewAndClearsTheTitleAndActionBar() {
        hold(blueprint("hall", null));
        sessions.follow();
        final TitleComponent title = lastTitle();
        final DisplayObject<Component> bar = actionBarEntry();
        when(titles.isShowing(title)).thenReturn(true);

        hold(air());
        sessions.follow();

        verify(previews).close(player);
        verify(actionBar).remove(bar);
        verify(titles).remove(title);
        verify(player).clearTitle();
    }

    @Test
    void ac4_swappingToAnotherBlueprintClosesTheOldPreviewAndOpensTheNew() {
        hold(blueprint("hall", null));
        sessions.follow();
        final TitleComponent title = lastTitle();
        final DisplayObject<Component> bar = actionBarEntry();

        hold(blueprint("workshop", null));
        sessions.follow();

        verify(previews).close(player);
        verify(actionBar).remove(bar);
        verify(titles).remove(title);
        assertEquals(2, opened.size());
        assertEquals(schematic("workshop_0"), openedFor.get(1));
    }

    @Test
    void ac4_loggingOutClosesThePreviewAndClearsTheTitleAndActionBar() {
        hold(blueprint("hall", null));
        sessions.follow();
        final TitleComponent title = lastTitle();
        final DisplayObject<Component> bar = actionBarEntry();

        final PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        sessions.onQuit(quit);

        verify(previews).close(player);
        verify(actionBar).remove(bar);
        verify(titles).remove(title);
    }

    @Test
    void ac5_thePreviewSitsOnTheBlockAboveTheOneLookedAtWithin24Blocks() {
        hold(blueprint("hall", null));

        sessions.follow();

        verify(player, atLeastOnce()).getTargetBlockExact(24);
        verify(opened.getFirst()).show(eq(anchor), anyInt());
    }

    @Test
    void ac5_lookingAtNothingHidesItAndSaysToLookAtTheGround() {
        when(player.getTargetBlockExact(anyInt())).thenReturn(null);
        hold(blueprint("hall", null));

        sessions.follow();

        verify(opened.getFirst()).hide();
        verify(opened.getFirst(), never()).show(any(), anyInt());
        assertTrue(ComponentKeys.hasKey(actionBarText(), "core.construction.blueprint.look_at_ground"));
    }

    @Test
    void ac6_thePreviewStartsFacingTheWayThePlayerFaces() {
        final int facing = BlockTransform.quarterTurnsBetween(0f, 90f);
        hold(blueprint("hall", null));

        sessions.follow();

        verify(opened.getFirst()).show(anchor, facing);
    }

    @Test
    void ac6_facingAnotherWayStartsItAnotherWay() {
        face(180);
        final int facing = BlockTransform.quarterTurnsBetween(0f, 180f);
        hold(blueprint("hall", null));

        sessions.follow();

        verify(opened.getFirst()).show(anchor, facing);
        assertFalse(facing == BlockTransform.quarterTurnsBetween(0f, 90f));
    }

    @Test
    void ac6_eachSneakTurnsItAQuarterAndTheTurnCarriesIntoThePlacement() {
        final int facing = BlockTransform.quarterTurnsBetween(0f, 90f);
        final StructureType hall = catalogue.find("hall").orElseThrow();
        when(construction.build(any(), any(), any(), any(), anyInt()))
                .thenReturn(ConstructionResult.refused(Component.text("no")));
        hold(blueprint("hall", null));
        sessions.follow();

        sneak(true);
        sneak(false);
        sneak(true);

        verify(opened.getFirst()).show(anchor, Math.floorMod(facing + 1, 4));
        verify(opened.getFirst()).show(anchor, Math.floorMod(facing + 2, 4));
        sessions.onUse(click(Action.RIGHT_CLICK_BLOCK));
        verify(construction).build(player, world, hall, anchor, Math.floorMod(facing + 2, 4));
    }

    @Test
    void ac7_aBuildBlueprintIsValidExactlyWhenTheBuildCheckFindsNoProblem() {
        final StructureType hall = catalogue.find("hall").orElseThrow();
        final int facing = BlockTransform.quarterTurnsBetween(0f, 90f);
        hold(blueprint("hall", null));

        sessions.follow();

        verify(checks, atLeastOnce()).problem(player, world, hall, anchor, facing);
        verify(checks, never()).moveProblem(any(), any(), any(), any(), anyInt());
        verify(opened.getFirst(), atLeastOnce()).setValid(true);
        verify(opened.getFirst(), never()).setValid(false);
    }

    @Test
    void ac7_aMoveBlueprintIsValidExactlyWhenTheMoveCheckFindsNoProblem() {
        final PlacedStructure placed = placed("hall", 0);
        final int facing = BlockTransform.quarterTurnsBetween(0f, 90f);
        hold(blueprint("hall", placed.getId()));

        sessions.follow();

        verify(checks, atLeastOnce()).moveProblem(player, world, placed.getId(), anchor, facing);
        verify(checks, never()).problem(any(), any(), any(), any(), anyInt());
        verify(opened.getFirst(), atLeastOnce()).setValid(true);
        verify(opened.getFirst(), never()).setValid(false);
    }

    @Test
    void ac7_whereItCannotGoTheTitleSaysSoAndTheActionBarGivesTheServicesReason() {
        final Component reason = Component.text("Too close to the well");
        when(checks.problem(any(), any(), any(), any(), anyInt())).thenReturn(Optional.of(reason));
        hold(blueprint("hall", null));

        sessions.follow();

        verify(opened.getFirst(), atLeastOnce()).setValid(false);
        verify(opened.getFirst(), never()).setValid(true);
        assertTrue(ComponentKeys.hasKey(subtitle(lastTitle()), "core.construction.blueprint.cannot_place")
                || ComponentKeys.hasKey(title(lastTitle()), "core.construction.blueprint.cannot_place"));
        assertEquals(reason, actionBarText());
    }

    @Test
    void ac7_aRefusedMoveShowsWhateverReasonTheMoveCheckGives() {
        final PlacedStructure placed = placed("hall", 0);
        final Component reason = Component.text("Something odd");
        when(checks.moveProblem(any(), any(), any(), any(), anyInt())).thenReturn(Optional.of(reason));
        hold(blueprint("hall", placed.getId()));

        sessions.follow();

        verify(opened.getFirst(), atLeastOnce()).setValid(false);
        assertEquals(reason, actionBarText());
    }

    @Test
    void ac9_rightClickingWithABlueprintNeverDoesAnythingElse() {
        hold(blueprint("hall", null));
        final PlayerInteractEvent beforeAPreview = click(Action.RIGHT_CLICK_AIR);
        sessions.onUse(beforeAPreview);
        verify(beforeAPreview).setCancelled(true);

        when(construction.build(any(), any(), any(), any(), anyInt()))
                .thenReturn(ConstructionResult.refused(Component.text("no")));
        sessions.follow();
        final PlayerInteractEvent onABlock = click(Action.RIGHT_CLICK_BLOCK);
        sessions.onUse(onABlock);
        verify(onABlock).setCancelled(true);
    }

    @Test
    void ac9_aBuildBlueprintAsksTheServiceToBuildAtThePreviewedSpotAndTurn() {
        final StructureType hall = catalogue.find("hall").orElseThrow();
        final int facing = BlockTransform.quarterTurnsBetween(0f, 90f);
        when(construction.build(any(), any(), any(), any(), anyInt()))
                .thenReturn(ConstructionResult.done(placed("hall", 0)));
        hold(blueprint("hall", null));
        sessions.follow();

        sessions.onUse(click(Action.RIGHT_CLICK_BLOCK));

        verify(construction).build(player, world, hall, anchor, facing);
        verify(construction, never()).move(any(), any(), any(), any(), anyInt());
    }

    @Test
    void ac9_aMoveBlueprintAsksTheServiceToMoveAtThePreviewedSpotAndTurn() {
        final PlacedStructure placed = placed("hall", 0);
        final int facing = BlockTransform.quarterTurnsBetween(0f, 90f);
        when(construction.move(any(), any(), any(), any(), anyInt())).thenReturn(ConstructionResult.done(placed));
        hold(blueprint("hall", placed.getId()));
        sessions.follow();

        sessions.onUse(click(Action.RIGHT_CLICK_AIR));

        verify(construction).move(player, world, placed.getId(), anchor, facing);
        verify(construction, never()).build(any(), any(), any(), any(), anyInt());
    }

    @Test
    void ac10_onSuccessOneBlueprintIsUsedThePreviewClosesAndThePlayerIsToldItStarted() {
        final ItemStack held = blueprint("hall", null);
        when(construction.build(any(), any(), any(), any(), anyInt()))
                .thenReturn(ConstructionResult.done(placed("hall", 0)));
        hold(held);
        sessions.follow();

        sessions.onUse(click(Action.RIGHT_CLICK_BLOCK));

        verify(held).subtract();
        verify(previews).close(player);
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.construction.blueprint.started"));
    }

    @Test
    void ac10_aMoveThatStartsIsAnnouncedAsAMove() {
        final PlacedStructure placed = placed("hall", 0);
        final ItemStack held = blueprint("hall", placed.getId());
        when(construction.move(any(), any(), any(), any(), anyInt())).thenReturn(ConstructionResult.done(placed));
        hold(held);
        sessions.follow();

        sessions.onUse(click(Action.RIGHT_CLICK_BLOCK));

        verify(held).subtract();
        assertTrue(ComponentKeys.hasKey(lastMessage(), "core.construction.blueprint.move_started"));
    }

    @Test
    void ac10_onRefusalTheBlueprintIsKeptAndThePlayerToldTheReason() {
        final ItemStack held = blueprint("hall", null);
        final Component reason = Component.text("Not enough wood");
        when(construction.build(any(), any(), any(), any(), anyInt())).thenReturn(ConstructionResult.refused(reason));
        hold(held);
        sessions.follow();

        sessions.onUse(click(Action.RIGHT_CLICK_BLOCK));

        verify(held, never()).subtract();
        verify(previews, never()).close(player);
        assertEquals("Not enough wood", ComponentKeys.text(lastMessage()));
    }

    private void face(float yaw) {
        when(player.getLocation()).thenReturn(new Location(world, 10, 65, 12, yaw, 0));
    }

    private void hold(@NotNull ItemStack stack) {
        when(inventory.getItemInMainHand()).thenReturn(stack);
    }

    private void sneak(boolean sneaking) {
        final PlayerToggleSneakEvent event = mock(PlayerToggleSneakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.isSneaking()).thenReturn(sneaking);
        sessions.onSneak(event);
    }

    private @NotNull PlayerInteractEvent click(@NotNull Action action) {
        final PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(action);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        return event;
    }

    private @NotNull ItemStack blueprint(@NotNull String type, @Nullable UUID moving) {
        final ItemStack stack = stack(Material.PAPER);
        final ItemInstance instance = mock(ItemInstance.class);
        when(instance.getBaseItem()).thenReturn(blueprintItem);
        when(instance.getComponent(StructureBlueprintComponent.class))
                .thenReturn(Optional.of(new StructureBlueprintComponent(type, moving)));
        when(itemFactory.fromItemStack(stack)).thenReturn(Optional.of(instance));
        return stack;
    }

    private @NotNull ItemStack otherItem() {
        final ItemStack stack = stack(Material.PAPER);
        final ItemInstance instance = mock(ItemInstance.class);
        when(instance.getBaseItem()).thenReturn(mock(BaseItem.class));
        when(instance.getComponent(StructureBlueprintComponent.class))
                .thenReturn(Optional.of(new StructureBlueprintComponent("hall")));
        when(itemFactory.fromItemStack(stack)).thenReturn(Optional.of(instance));
        return stack;
    }

    private static @NotNull ItemStack air() {
        return stack(Material.AIR);
    }

    private static @NotNull ItemStack stack(@NotNull Material material) {
        final ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        return stack;
    }

    private @NotNull PlacedStructure placed(@NotNull String type, int stage) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        structure.setStage(stage);
        holding.getStructures().add(structure);
        return structure;
    }

    private @NotNull Schematic schematic(@NotNull String name) {
        return schematics.load(name).orElseThrow();
    }

    private @NotNull TitleComponent lastTitle() {
        final ArgumentCaptor<TitleComponent> captor = ArgumentCaptor.forClass(TitleComponent.class);
        verify(titles, atLeastOnce()).add(eq(10), captor.capture());
        return captor.getValue();
    }

    private @NotNull Component title(@NotNull TitleComponent title) {
        return title.getProvider().apply(gamer);
    }

    private @NotNull Component subtitle(@NotNull TitleComponent title) {
        return title.getSubtitleProvider().apply(gamer);
    }

    @SuppressWarnings("unchecked")
    private @NotNull DisplayObject<Component> actionBarEntry() {
        final ArgumentCaptor<DisplayObject<Component>> captor = ArgumentCaptor.forClass(DisplayObject.class);
        verify(actionBar, atLeastOnce()).add(eq(50), captor.capture());
        return captor.getValue();
    }

    private @NotNull Component actionBarText() {
        return actionBarEntry().getProvider().apply(gamer);
    }

    private @NotNull Component lastMessage() {
        final ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        return captor.getValue();
    }
}
