package me.mykindos.betterpvp.core.world.construction.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import me.mykindos.betterpvp.core.world.construction.StructurePlacedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.schematic.SchematicRenderer;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostShell;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shows every loaded holding's structures in its world and keeps them current: raising builds as their jobs progress,
 * announcing statuses as jobs finish, and putting structures up and taking them down as they are built, claimed or
 * demolished.
 * <p>
 * The module that owns a kind of site binds {@link #content()} to that site's worlds, alongside
 * {@link me.mykindos.betterpvp.core.world.construction.BuildZones}.
 */
@BPvPListener
@Singleton
public class StructureViews implements Listener {

    private final ConstructionService service;
    private final ConstructionSites sites;
    private final StructureStatusTracker tracker;
    private final StructureCatalogue catalogue;
    private final SiteInstances instances;
    @Getter(AccessLevel.PACKAGE)
    private final StructureShapes shapes;
    @Getter(AccessLevel.PACKAGE)
    private final SchematicRenderer renderer;
    @Getter(AccessLevel.PACKAGE)
    private final SceneObjectRegistry registry;
    @Getter(AccessLevel.PACKAGE)
    private final ConstructionPropFactory propFactory;
    @Getter(AccessLevel.PACKAGE)
    private final GhostShell shell = GhostShell.standard();

    private final Map<String, Loaded> worlds = new HashMap<>();

    @Inject
    public StructureViews(@NotNull ConstructionService service, @NotNull ConstructionSites sites,
                          @NotNull StructureStatusTracker tracker, @NotNull StructureCatalogue catalogue,
                          @NotNull SiteInstances instances, @NotNull StructureShapes shapes,
                          @NotNull SchematicRenderer renderer, @NotNull SceneObjectRegistry registry,
                          @NotNull ConstructionPropFactory propFactory) {
        this.service = service;
        this.sites = sites;
        this.tracker = tracker;
        this.catalogue = catalogue;
        this.instances = instances;
        this.shapes = shapes;
        this.renderer = renderer;
        this.registry = registry;
        this.propFactory = propFactory;
    }

    /** Content showing the structures of whatever holding a world belongs to. */
    public @NotNull WorldContent content() {
        return new WorldContent() {
            @Override
            public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
                final Loaded loaded = new Loaded(scope);
                worlds.put(world.getName(), loaded);
                scope.onRelease(() -> {
                    worlds.remove(world.getName(), loaded);
                    loaded.views.values().forEach(StructureView::release);
                });
                refresh(world, loaded);
            }
        };
    }

    @UpdateEvent(delay = 1000)
    public void tick() {
        for (Map.Entry<String, Loaded> entry : new ArrayList<>(worlds.entrySet())) {
            final World world = Bukkit.getWorld(entry.getKey());
            if (world != null) {
                refresh(world, entry.getValue());
                entry.getValue().views.values().forEach(StructureView::blink);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlaced(@NotNull StructurePlacedEvent event) {
        forSite(event.getSite(), (world, loaded) -> sync(world, loaded, event.getSite(), event.getStructure()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatusChange(@NotNull StructureStatusChangeEvent event) {
        forSite(event.getSite(), (world, loaded) -> sync(world, loaded, event.getSite(), event.getStructure()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemoved(@NotNull StructureRemovedEvent event) {
        forSite(event.getSite(), (world, loaded) -> {
            final StructureView view = loaded.views.remove(event.getStructure().getId());
            if (view != null) {
                view.clear();
            }
        });
    }

    /**
     * Right-clicks on a structure being built or waiting to be claimed do nothing, so its containers stay shut and its
     * pieces stay still. Right-clicking an upgrade's piece on an Active structure lets whatever the upgrade does take
     * the click, for whoever may use it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(@NotNull PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) {
            return;
        }
        final StructureView view = viewAt(block);
        if (view == null || view.getStructure() == null) {
            return;
        }
        final PlacedStructure structure = view.getStructure();
        final StructureStatus status = structure.status(tracker.now());
        if (status == StructureStatus.READY_TO_CLAIM
                || structure.getCondition() == StructureCondition.UNDER_CONSTRUCTION) {
            event.setCancelled(true);
            return;
        }
        view.pieceAt(block.getX(), block.getY(), block.getZ())
                .ifPresent(id -> usePiece(event, block.getWorld(), structure, status, id));
    }

    /** The structure whose build or upgrade piece stands on {@code block}, if one is shown there. */
    public @NotNull Optional<PlacedStructure> structureAt(@NotNull Block block) {
        return Optional.ofNullable(viewAt(block)).map(StructureView::getStructure);
    }

    private @Nullable StructureView viewAt(@NotNull Block block) {
        final Loaded loaded = worlds.get(block.getWorld().getName());
        if (loaded == null) {
            return null;
        }
        for (StructureView view : loaded.views.values()) {
            if (view.getStructure() != null && (view.covers(block.getX(), block.getY(), block.getZ())
                    || view.pieceAt(block.getX(), block.getY(), block.getZ()).isPresent())) {
                return view;
            }
        }
        return null;
    }

    private void usePiece(@NotNull PlayerInteractEvent event, @NotNull World world, @NotNull PlacedStructure structure,
                          @NotNull StructureStatus status, @NotNull String upgrade) {
        if (!status.isUsable()) {
            event.setCancelled(true);
            return;
        }
        sites.worksite(world).ifPresent(worksite -> catalogue.find(structure.getType())
                .flatMap(type -> type.upgrade(upgrade))
                .ifPresent(found -> {
                    if (!sites.canUse(event.getPlayer(), worksite.getKey(), structure)) {
                        event.setCancelled(true);
                        UtilMessage.plain(event.getPlayer(),
                                ConstructionResult.reason("core.construction.not_yours"));
                        return;
                    }
                    final StructurePieceUseEvent use = new StructurePieceUseEvent(event.getPlayer(),
                            worksite.getKey(), structure, found);
                    UtilServer.callEvent(use);
                    if (use.isHandled()) {
                        event.setCancelled(true);
                    }
                }));
    }

    /** Writes down a structure container when whoever had it open closes it. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder(false);
        if (holder instanceof DoubleChest chest) {
            saveAt(chest.getLeftSide(false));
            saveAt(chest.getRightSide(false));
        } else {
            saveAt(holder);
        }
    }

    private void saveAt(@Nullable InventoryHolder holder) {
        if (!(holder instanceof BlockState state)) {
            return;
        }
        final Loaded loaded = worlds.get(state.getWorld().getName());
        if (loaded == null) {
            return;
        }
        for (StructureView view : loaded.views.values()) {
            if (view.saveAt(state.getX(), state.getY(), state.getZ())) {
                return;
            }
        }
    }

    /** Marks the record of whatever holding {@code world} belongs to as needing a save. */
    void changed(@NotNull World world) {
        sites.worksite(world).ifPresent(worksite -> worksite.getSite().changed(worksite.getKey()));
    }

    void claim(@NotNull Player player, @NotNull World world, @NotNull UUID structure) {
        final ConstructionResult result = service.claim(player, world, structure);
        if (!result.isSuccess() && result.getReason() != null) {
            UtilMessage.plain(player, result.getReason());
        }
    }

    private void refresh(@NotNull World world, @NotNull Loaded loaded) {
        sites.worksite(world).ifPresent(worksite -> {
            tracker.refresh(worksite);
            worksite.getHolding().getStructures()
                    .forEach(structure -> sync(world, loaded, worksite.getKey(), structure));
        });
    }

    private void sync(@NotNull World world, @NotNull Loaded loaded, @NotNull SiteKey site,
                      @NotNull PlacedStructure structure) {
        catalogue.find(structure.getType()).ifPresent(type -> {
            loaded.views.computeIfAbsent(structure.getId(), id -> new StructureView(this, world, loaded.scope))
                    .sync(structure, type, tracker.now());
        });
    }

    private void forSite(@NotNull SiteKey site, @NotNull WorldAction action) {
        for (SiteInstance instance : instances.forKey(site)) {
            final Loaded loaded = worlds.get(instance.getWorldName());
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (loaded != null && world != null) {
                action.run(world, loaded);
            }
        }
    }

    @FunctionalInterface
    private interface WorldAction {
        void run(@NotNull World world, @NotNull Loaded loaded);
    }

    /** One world's structure views, and the content scope they live in. */
    private static final class Loaded {
        private final WorldContentScope scope;
        private final Map<UUID, StructureView> views = new HashMap<>();

        private Loaded(@NotNull WorldContentScope scope) {
            this.scope = scope;
        }
    }
}
