package me.mykindos.betterpvp.core.world.construction.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import me.mykindos.betterpvp.core.world.construction.StructurePlacedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
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
    public StructureViews(@NotNull ConstructionService service, @NotNull StructureCatalogue catalogue,
                          @NotNull SiteInstances instances, @NotNull StructureShapes shapes,
                          @NotNull SchematicRenderer renderer, @NotNull SceneObjectRegistry registry,
                          @NotNull ConstructionPropFactory propFactory) {
        this.service = service;
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

    /** Right-clicking an upgrade's piece lets whatever the upgrade does take the click. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(@NotNull PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) {
            return;
        }
        final Loaded loaded = worlds.get(block.getWorld().getName());
        if (loaded == null) {
            return;
        }
        for (Map.Entry<UUID, StructureView> entry : loaded.views.entrySet()) {
            final Optional<String> upgrade = entry.getValue().pieceAt(block.getX(), block.getY(), block.getZ());
            if (upgrade.isEmpty()) {
                continue;
            }
            service.worksite(block.getWorld()).ifPresent(worksite -> worksite.getHolding().find(entry.getKey())
                    .ifPresent(structure -> catalogue.find(structure.getType())
                            .flatMap(type -> type.upgrade(upgrade.get()))
                            .ifPresent(found -> {
                                final StructurePieceUseEvent use = new StructurePieceUseEvent(event.getPlayer(),
                                        worksite.getKey(), structure, found);
                                UtilServer.callEvent(use);
                                if (use.isHandled()) {
                                    event.setCancelled(true);
                                }
                            })));
            return;
        }
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
        service.worksite(world).ifPresent(worksite -> worksite.getSite().changed(worksite.getKey()));
    }

    void claim(@NotNull Player player, @NotNull World world, @NotNull UUID structure) {
        final ConstructionResult result = service.claim(player, world, structure);
        if (!result.isSuccess() && result.getReason() != null) {
            UtilMessage.message(player, Translations.component("core.prefix.construction"), result.getReason());
        }
    }

    private void refresh(@NotNull World world, @NotNull Loaded loaded) {
        service.worksite(world).ifPresent(worksite -> {
            service.refresh(worksite);
            worksite.getHolding().getStructures()
                    .forEach(structure -> sync(world, loaded, worksite.getKey(), structure));
        });
    }

    private void sync(@NotNull World world, @NotNull Loaded loaded, @NotNull SiteKey site,
                      @NotNull PlacedStructure structure) {
        catalogue.find(structure.getType()).ifPresent(type -> {
            loaded.views.computeIfAbsent(structure.getId(), id -> new StructureView(this, world, loaded.scope))
                    .sync(structure, type, service.now());
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
