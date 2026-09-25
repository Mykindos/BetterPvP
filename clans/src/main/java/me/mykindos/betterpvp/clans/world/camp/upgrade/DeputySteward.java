package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.hall.HallMenus;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerModels;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
import me.mykindos.betterpvp.core.world.construction.StructureUpgradedEvent;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Great Hall upgrade: a second Steward the clan places anywhere in its camp. Right-clicking it opens the same Great Hall
 * menu, but only while the Great Hall is working. It stands where the camp record says for as long as the Great Hall
 * has the upgrade, and is dismissed for good once it no longer does.
 * <p>
 * It can be placed where a member stands in their own camp, on solid ground with room to stand, and outside every
 * structure's footprint.
 */
@BPvPListener
@Singleton
public class DeputySteward implements Listener {

    public static final String ID = "deputy_steward";

    private final CampUpgrades upgrades;
    private final CampStore store;
    private final CampPermissions permissions;
    private final ConstructionService construction;
    private final StructureShapes shapes;
    private final ClansSceneObjectFactory factory;
    private final SettlerConfig config;
    private final SettlerModels models;
    private final HallMenus menus;
    private final Map<String, Placed> worlds = new HashMap<>();

    @Inject
    public DeputySteward(@NotNull CampUpgrades upgrades, @NotNull CampStore store,
                         @NotNull CampPermissions permissions, @NotNull ConstructionService construction,
                         @NotNull StructureShapes shapes, @NotNull ClansSceneObjectFactory factory,
                         @NotNull SettlerConfig config, @NotNull SettlerModels models, @NotNull HallMenus menus) {
        this.upgrades = upgrades;
        this.store = store;
        this.permissions = permissions;
        this.construction = construction;
        this.shapes = shapes;
        this.factory = factory;
        this.config = config;
        this.models = models;
        this.menus = menus;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 3);
        upgrades.page(ID, (player, camp, structure, previous) -> new DeputyStewardMenu(this, camp, previous).show(player));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    /** Whether camp {@code key} has a Deputy placed. */
    public boolean isPlaced(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId()).map(Camp::getDeputy).isPresent();
    }

    /** Content putting the Deputy in whatever camp a world belongs to. */
    public @NotNull WorldContent content() {
        return new WorldContent() {
            @Override
            public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
                final Placed placed = new Placed(scope);
                worlds.put(world.getName(), placed);
                scope.onRelease(() -> worlds.remove(world.getName(), placed));
                sync(world, placed);
            }
        };
    }

    /**
     * Places the Deputy where {@code player} stands.
     *
     * @return the translation key of why it was refused, or null once done
     */
    public @Nullable String place(@NotNull Player player, @NotNull SiteKey key) {
        final String refused = refusal(player, key);
        if (refused != null) {
            return refused;
        }
        final Location at = player.getLocation();
        final Holding holding = construction.worksite(at.getWorld())
                .filter(worksite -> worksite.getKey().equals(key))
                .map(ConstructionService.Worksite::getHolding)
                .orElse(null);
        final Block feet = at.getBlock();
        final boolean standable = feet.getRelative(BlockFace.DOWN).isSolid() && feet.isPassable()
                && feet.getRelative(BlockFace.UP).isPassable();
        final String problem = placementProblem(holding != null, standable,
                holding == null ? List.of() : footprints(at.getWorld(), holding), at.getBlockX(), at.getBlockZ());
        if (problem != null) {
            return problem;
        }
        store.cached(key.getOwnerId()).ifPresent(camp -> {
            camp.setDeputy(DeputyPost.of(at));
            store.changed(key.getOwnerId());
        });
        sync(at.getWorld().getName());
        return null;
    }

    /**
     * Sends the Deputy away until it is placed again.
     *
     * @return the translation key of why it was refused, or null once done
     */
    public @Nullable String dismiss(@NotNull Player player, @NotNull SiteKey key) {
        final String refused = refusal(player, key);
        if (refused != null) {
            return refused;
        }
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null || camp.getDeputy() == null) {
            return "clans.camp.upgrade.deputy_steward.not_placed";
        }
        camp.setDeputy(null);
        store.changed(key.getOwnerId());
        tick();
        return null;
    }

    private @Nullable String refusal(@NotNull Player player, @NotNull SiteKey key) {
        if (store.cached(key.getOwnerId()).isEmpty()) {
            return "core.settler.not_loaded";
        }
        if (!isActive(key)) {
            return "clans.camp.upgrade.deputy_steward.inactive";
        }
        if (!permissions.allows(player, key.getOwnerId(), ConstructionAction.MOVE)) {
            return "clans.settler.card.not_allowed";
        }
        return null;
    }

    /**
     * Why the Deputy cannot stand at column {@code (x, z)}, or null if it can.
     *
     * @param ownCamp   whether the spot is in the camp placing it
     * @param standable whether there is solid ground below and room to stand
     * @param occupied  the footprints of the camp's structures
     */
    static @Nullable String placementProblem(boolean ownCamp, boolean standable, @NotNull List<Footprint> occupied,
                                             int x, int z) {
        if (!ownCamp) {
            return "clans.camp.upgrade.deputy_steward.not_here";
        }
        if (!standable) {
            return "clans.camp.upgrade.deputy_steward.no_ground";
        }
        if (occupied.stream().anyMatch(footprint -> footprint.containsColumn(x, z))) {
            return "clans.camp.upgrade.deputy_steward.in_structure";
        }
        return null;
    }

    private @NotNull List<Footprint> footprints(@NotNull World world, @NotNull Holding holding) {
        final List<Footprint> footprints = new ArrayList<>();
        for (PlacedStructure structure : holding.getStructures()) {
            if (structure.getCondition() != StructureCondition.NOT_PLACED) {
                shapes.footprintOf(world, structure.getType(), structure.getStage(), structure.getPosition())
                        .ifPresent(footprints::add);
            }
        }
        return footprints;
    }

    @UpdateEvent(delay = 5000)
    public void tick() {
        new ArrayList<>(worlds.keySet()).forEach(this::sync);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatusChange(@NotNull StructureStatusChangeEvent event) {
        syncIfHall(event.getStructure());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemoved(@NotNull StructureRemovedEvent event) {
        syncIfHall(event.getStructure());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpgraded(@NotNull StructureUpgradedEvent event) {
        syncIfHall(event.getStructure());
    }

    private void syncIfHall(@NotNull PlacedStructure structure) {
        if (structure.getType().equals(CampConstruction.GREAT_HALL)) {
            tick();
        }
    }

    private void sync(@NotNull String worldName) {
        final World world = Bukkit.getWorld(worldName);
        final Placed placed = worlds.get(worldName);
        if (world != null && placed != null) {
            sync(world, placed);
        }
    }

    /** Puts the Deputy where the camp record says, or takes it away, forgetting its spot once the upgrade is gone. */
    private void sync(@NotNull World world, @NotNull Placed placed) {
        if (placed.scope.isReleased()) {
            return;
        }
        final ConstructionService.Worksite worksite = construction.worksite(world).orElse(null);
        final Camp camp = worksite == null ? null : store.cached(worksite.getKey().getOwnerId()).orElse(null);
        Location wanted = null;
        if (camp != null && camp.getDeputy() != null) {
            final boolean fitted = worksite.getHolding().ofType(CampConstruction.GREAT_HALL).stream()
                    .anyMatch(hall -> hall.hasUpgrade(ID));
            if (fitted) {
                wanted = camp.getDeputy().toLocation(world);
            } else {
                camp.setDeputy(null);
                store.changed(worksite.getKey().getOwnerId());
            }
        }
        if (Objects.equals(wanted, placed.at)) {
            return;
        }

        if (placed.npc != null) {
            placed.npc.remove();
            placed.npc = null;
        }
        placed.at = wanted;
        if (wanted == null) {
            return;
        }
        final SiteKey key = worksite.getKey();
        final ModeledNPC npc = new ModeledNPC(factory);
        npc.addDecorator(object -> dress(npc));
        npc.setInteractionHandler(player -> interact(player, key));
        placed.scope.add(new SceneSpawn(npc, wanted, factory::backingEntity));
        placed.npc = npc;
    }

    private void interact(@NotNull Player player, @NotNull SiteKey key) {
        if (!isActive(key)) {
            UtilMessage.plain(player, Translations.component("clans.camp.upgrade.deputy_steward.hall_inactive")
                            .color(NamedTextColor.RED));
            return;
        }
        menus.openHub(player, key);
    }

    private void dress(@NotNull ModeledNPC npc) {
        models.dress(npc, config.look("steward"),
                Translations.component("clans.camp.upgrade.deputy_steward.npc").color(NamedTextColor.GOLD),
                Translations.component("clans.camp.hall.steward_role").color(NamedTextColor.YELLOW));
    }

    private static final class Placed {

        private final WorldContentScope scope;
        private @Nullable ModeledNPC npc;
        private @Nullable Location at;

        private Placed(@NotNull WorldContentScope scope) {
            this.scope = scope;
        }
    }
}
