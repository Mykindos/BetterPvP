package me.mykindos.betterpvp.clans.world.camp.hall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerModels;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePlacedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The one NPC who runs a camp's Great Hall. Right-clicking the Steward opens everything a clan manages from the hall:
 * wages, crews, construction and permissions. The Steward stands on the Great Hall's {@code steward} point and moves
 * with the hall when it is moved or rebuilt, and is not there while the hall is not standing.
 */
@BPvPListener
@Singleton
public class Steward implements Listener {

    /** The point in a Great Hall build the Steward stands on. */
    public static final String POINT = "steward";
    private static final String LOOK = "steward";

    private final ConstructionService construction;
    private final StructureShapes shapes;
    private final ClansSceneObjectFactory factory;
    private final SettlerConfig config;
    private final HallMenus menus;
    private final SettlerModels models;
    private final Map<String, Placed> worlds = new HashMap<>();

    @Inject
    public Steward(@NotNull ConstructionService construction, @NotNull StructureShapes shapes,
                   @NotNull ClansSceneObjectFactory factory, @NotNull SettlerConfig config, @NotNull HallMenus menus,
                   @NotNull SettlerModels models) {
        this.construction = construction;
        this.shapes = shapes;
        this.factory = factory;
        this.config = config;
        this.menus = menus;
        this.models = models;
    }

    /** Content putting the Steward in whatever camp a world belongs to. */
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

    @UpdateEvent(delay = 5000)
    public void tick() {
        new ArrayList<>(worlds.keySet()).forEach(this::sync);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlaced(@NotNull StructurePlacedEvent event) {
        syncIfHall(event.getStructure());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatusChange(@NotNull StructureStatusChangeEvent event) {
        syncIfHall(event.getStructure());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemoved(@NotNull StructureRemovedEvent event) {
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

    /** Moves the Steward to where the hall now wants it, or takes it away if the hall is not standing. */
    private void sync(@NotNull World world, @NotNull Placed placed) {
        if (placed.scope.isReleased()) {
            return;
        }
        final Location wanted = construction.worksite(world).flatMap(worksite -> standing(worksite.getHolding()
                        .ofType(CampConstruction.GREAT_HALL))
                .map(hall -> shapes.point(world, hall, POINT)
                        .orElseGet(() -> hall.getPosition().toLocation(world).add(0.5, 1, 0.5)))
                .map(Steward::standAt))
                .orElse(null);
        if (sameSpot(wanted, placed.at)) {
            return;
        }

        if (placed.npc != null) {
            placed.npc.remove();
            placed.npc = null;
        }
        placed.at = wanted;
        final SiteKey key = construction.worksite(world).map(ConstructionService.Worksite::getKey).orElse(null);
        if (wanted == null || key == null) {
            return;
        }
        final ModeledNPC npc = new ModeledNPC(factory);
        npc.addDecorator(object -> dress(npc));
        npc.setInteractionHandler(player -> menus.openHub(player, key));
        placed.scope.add(new SceneSpawn(npc, wanted, factory::backingEntity));
        placed.npc = npc;
    }

    /** The middle of the point's block, facing the way the point faces. */
    private static @NotNull Location standAt(@NotNull Location point) {
        final Location at = point.toBlockLocation().add(0.5, 0, 0.5);
        at.setYaw(point.getYaw());
        at.setPitch(0);
        return at;
    }

    private static @NotNull Optional<PlacedStructure> standing(@NotNull List<PlacedStructure> halls) {
        return halls.stream()
                .filter(hall -> hall.getCondition() != StructureCondition.NOT_PLACED
                        && hall.getCondition() != StructureCondition.UNDER_CONSTRUCTION)
                .findFirst();
    }

    private void dress(@NotNull ModeledNPC npc) {
        models.dress(npc, config.look(LOOK),
                Translations.component("clans.camp.hall.steward").color(NamedTextColor.GOLD),
                Translations.component("clans.camp.hall.steward_role").color(NamedTextColor.YELLOW));
    }

    private static boolean sameSpot(@Nullable Location a, @Nullable Location b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
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
