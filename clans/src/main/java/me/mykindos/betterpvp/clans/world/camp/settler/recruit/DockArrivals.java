package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import me.mykindos.betterpvp.clans.scene.ClansSceneObjectFactory;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampSettlers;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerModels;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerCards;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Candidates waiting at a camp's Dock, standing in a ring around its {@code settler_arrival} point. Right-clicking
 * one shows who they are and what they ask. They go when they are hired, sent away, or tired of waiting.
 */
@BPvPListener
@Singleton
public class DockArrivals implements Listener {

    /** The point candidates wait around, in the Dock's build or on the island. */
    public static final String POINT = "settler_arrival";

    private final CampRecruitment recruitment;
    private final ConstructionService construction;
    private final StructureShapes shapes;
    private final ClansSceneObjectFactory factory;
    private final SettlerModels models;
    private final CampSettlers campSettlers;
    private final SettlerCards cards;
    private final Camps camps;
    private final SiteInstances instances;
    private final Map<String, Waiting> worlds = new HashMap<>();

    @Inject
    public DockArrivals(@NotNull CampRecruitment recruitment, @NotNull ConstructionService construction,
                        @NotNull StructureShapes shapes, @NotNull ClansSceneObjectFactory factory,
                        @NotNull SettlerModels models, @NotNull CampSettlers campSettlers,
                        @NotNull SettlerCards cards, @NotNull Camps camps, @NotNull SiteInstances instances) {
        this.recruitment = recruitment;
        this.construction = construction;
        this.shapes = shapes;
        this.factory = factory;
        this.models = models;
        this.campSettlers = campSettlers;
        this.cards = cards;
        this.camps = camps;
        this.instances = instances;
    }

    /** Content showing the candidates waiting at whatever camp a world belongs to. */
    public @NotNull WorldContent content() {
        return new WorldContent() {
            @Override
            public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
                instances.byWorld(world.getName()).map(SiteInstance::getKey).ifPresent(key -> {
                    final Waiting waiting = new Waiting(key, regions, scope);
                    worlds.put(world.getName(), waiting);
                    scope.onRelease(() -> worlds.remove(world.getName(), waiting));
                    sync(world, waiting);
                });
            }
        };
    }

    @UpdateEvent(delay = 5000)
    public void tick() {
        for (Map.Entry<String, Waiting> entry : new ArrayList<>(worlds.entrySet())) {
            final World world = Bukkit.getWorld(entry.getKey());
            if (world != null) {
                sync(world, entry.getValue());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBoat(@NotNull SettlerBoatEvent event) {
        tick();
    }

    private void sync(@NotNull World world, @NotNull Waiting waiting) {
        if (waiting.scope.isReleased()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final List<SettlerCandidate> candidates = recruitment.arrivals(waiting.key).stream()
                .filter(candidate -> !candidate.isExpired(now))
                .toList();
        final Set<UUID> present = new HashSet<>();
        candidates.forEach(candidate -> present.add(candidate.getSettler().getId()));
        waiting.npcs.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });

        final Location point = point(world, waiting).orElse(null);
        if (point == null) {
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            final SettlerCandidate candidate = candidates.get(i);
            if (!waiting.npcs.containsKey(candidate.getSettler().getId())) {
                waiting.npcs.put(candidate.getSettler().getId(), spawn(waiting, candidate, spot(point, i)));
            }
        }
    }

    private @NotNull ModeledNPC spawn(@NotNull Waiting waiting, @NotNull SettlerCandidate candidate,
                                      @NotNull Location at) {
        final Settler settler = candidate.getSettler();
        final long price = recruitment.price(waiting.key, candidate);
        final Component role = price <= 0
                ? Translations.component("clans.settler.recruit.free").color(NamedTextColor.GREEN)
                : Translations.component("clans.settler.recruit.price",
                Component.text(UtilFormat.formatNumber((int) price))).color(NamedTextColor.GOLD);
        final ModeledNPC npc = new ModeledNPC(factory);
        npc.addDecorator(object -> models.dress(npc, campSettlers.look(waiting.key, settler),
                Component.text(settler.getName(), settler.getRarity().getColor()), role));
        npc.setInteractionHandler(player -> open(player, waiting.key, settler.getId()));
        waiting.scope.add(new SceneSpawn(npc, at, factory::backingEntity));
        return npc;
    }

    private void open(@NotNull Player player, @NotNull SiteKey key, @NotNull UUID id) {
        if (!camps.isMember(player, player.getWorld())) {
            UtilMessage.plain(player, Translations.component("clans.settler.recruit.members_only").color(NamedTextColor.GRAY));
            return;
        }
        cards.openCandidate(player, key, id, null);
    }

    /** The Dock's own point, then the island's, then just above where the Dock stands. */
    private @NotNull Optional<Location> point(@NotNull World world, @NotNull Waiting waiting) {
        final Optional<PlacedStructure> dock = construction.worksite(world)
                .flatMap(worksite -> worksite.getHolding().ofType(CampStructures.DOCK).stream().findFirst());
        return dock.flatMap(found -> shapes.point(world, found, POINT))
                .or(() -> waiting.regions.find(POINT, PointRegion.class).stream().findFirst()
                        .map(region -> region.getLocation().clone()))
                .or(() -> dock.map(found -> found.getPosition().toLocation(world).add(0.5, 1, 0.5)));
    }

    /** Candidates stand in a ring, one more step out for every eight. */
    private static @NotNull Location spot(@NotNull Location point, int index) {
        final double radius = 1.5 + index / 8;
        final double angle = Math.PI * 2 * (index % 8) / 8.0;
        final Location at = point.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        at.setYaw(point.getYaw());
        return at;
    }

    private static final class Waiting {

        private final SiteKey key;
        private final RegionIndex regions;
        private final WorldContentScope scope;
        private final Map<UUID, ModeledNPC> npcs = new HashMap<>();

        private Waiting(@NotNull SiteKey key, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
            this.key = key;
            this.regions = regions;
            this.scope = scope;
        }
    }
}
