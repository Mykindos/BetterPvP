package me.mykindos.betterpvp.clans.world.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Installs {@link WorldContent} into worlds as they come and go.
 * <p>
 * Content declares which worlds it belongs to through a {@link WorldSelector}, so the same content can be authored once
 * and appear in every world that matches - including worlds that did not exist when it was declared. That is what makes
 * a set of residents or props plug-and-play across islands rather than something that has to be wired up per world.
 * <p>
 * Loading is scoped to one world at a time. This matters more than it sounds: the loaders this replaced could only
 * rebuild <em>everything</em>, so a single instanced island appearing meant tearing down and re-spawning content in
 * every other world too - which with live instances is both a stall and a way to destroy state somebody is standing in.
 * A world that appears is built, and a world that goes away has exactly its own content removed.
 *
 * @see WorldContentListener for what drives it
 */
@CustomLog
@Singleton
public class WorldContentService implements Reloadable {

    private final ZoneManager zoneManager;
    private final SceneObjectRegistry sceneRegistry;

    private final List<WorldContentBinding> bindings = new ArrayList<>();
    private final List<RegionContributor> contributors = new ArrayList<>();
    private final Map<String, WorldState> loaded = new HashMap<>();

    /** Until the first sweep, worlds are ignored - Mapper and the content declaring itself are not ready yet. */
    private boolean started;

    @Inject
    public WorldContentService(@NotNull ZoneManager zoneManager, @NotNull SceneObjectRegistry sceneRegistry,
                               @NotNull Clans clans) {
        this.zoneManager = zoneManager;
        this.sceneRegistry = sceneRegistry;
        clans.getReloadables().add(this);
    }

    /**
     * Declares content and the worlds it belongs to. Registering after the server has started applies it immediately to
     * every world already loaded, so content that is wired up late is not silently missing until the next reload.
     */
    public void register(@NotNull WorldContentBinding binding) {
        bindings.add(binding);
        if (!started) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            if (!binding.matches(world)) {
                continue;
            }
            readIndex(world).ifPresent(regions ->
                    apply(binding, world, regions, loaded.computeIfAbsent(world.getName(), key -> new WorldState())));
        }
    }

    /**
     * Registers a source of data-points that are not in a world's Mapper file, such as a structure pasted at runtime.
     * Contributors run before any content is asked about a world, so what they add is indistinguishable from an
     * authored marker.
     */
    public void registerRegions(@NotNull RegionContributor contributor) {
        contributors.add(contributor);
    }

    /** Builds every world currently loaded. Called once the server has finished starting. */
    public void start() {
        started = true;
        Bukkit.getWorlds().forEach(this::loadWorld);
    }

    /**
     * Rebuilds one world's content from scratch. Safe to call on a world that is already built - its previous content
     * is removed first - which is what lets the world-load event and the start-up sweep both fire without doubling up.
     */
    public void loadWorld(@NotNull World world) {
        if (!started || bindings.isEmpty()) {
            return;
        }

        unloadWorld(world);
        final List<WorldContentBinding> matching = bindings.stream().filter(binding -> binding.matches(world)).toList();
        if (matching.isEmpty()) {
            return;
        }

        readIndex(world).ifPresent(regions -> {
            final WorldState state = new WorldState();
            loaded.put(world.getName(), state);
            matching.forEach(binding -> apply(binding, world, regions, state));
            log.info("Loaded {} scene object(s) and {} zone(s) into '{}'",
                    state.objects.size(), state.zones.size(), world.getName()).submit();
        });
    }

    /** Removes everything this service put into {@code world}, leaving other worlds untouched. */
    public void unloadWorld(@NotNull World world) {
        unloadWorld(world.getName());
    }

    @Override
    public void reload() {
        if (!started) {
            return;
        }

        // By name, not by World: a world may already be gone from Bukkit, and its objects still need unregistering.
        new ArrayList<>(loaded.keySet()).forEach(this::unloadWorld);
        Bukkit.getWorlds().forEach(this::loadWorld);
    }

    private void unloadWorld(@NotNull String worldName) {
        final WorldState state = loaded.remove(worldName);
        if (state == null) {
            return;
        }

        state.objects.forEach(SceneObject::remove);
        state.zones.forEach(zoneManager::unregister);
    }

    /**
     * Installs one bundle's zones and scene objects. Each piece of content is isolated, because this now runs across
     * every world on the server: one bad data-point should cost its own content, not the rest of the world's.
     */
    private void apply(@NotNull WorldContentBinding binding, @NotNull World world, @NotNull RegionIndex regions,
                       @NotNull WorldState state) {
        for (WorldContent content : binding.getContent().get()) {
            try {
                for (Zone zone : content.zones(world, regions)) {
                    zoneManager.register(zone);
                    state.zones.add(zone);
                }
                for (SceneSpawn spawn : content.sceneObjects(world, regions)) {
                    // Isolated per object, not per content: a dock's navigator and its zone come from the same bundle,
                    // and one NPC that cannot build itself used to silently take the rest of the dock with it.
                    try {
                        // Chunk-managed: the body is spawned by entityFactory when the anchor chunk loads, not now.
                        spawn.getObject().configureMaterialization(spawn.getAnchor(), spawn.getEntityFactory());
                        sceneRegistry.register(spawn.getObject());
                        state.objects.add(spawn.getObject());
                    } catch (Exception exception) {
                        log.error("Scene object from {} failed to load at {}",
                                content.getClass().getSimpleName(), spawn.getAnchor(), exception).submit();
                    }
                }
            } catch (Exception exception) {
                log.error("Content {} failed to load into '{}'",
                        content.getClass().getSimpleName(), world.getName(), exception).submit();
            }
        }
    }

    /**
     * Builds the world's data-point index: what is authored on disk, plus whatever the contributors put there this
     * load. A world with no Mapper file still gets an index if something contributed to it, which is what lets a
     * freshly cloned world carry only a pasted structure.
     */
    private Optional<RegionIndex> readIndex(@NotNull World world) {
        // A map still being authored gets nothing at all - not even the contributors, whose structures would otherwise
        // paste themselves over whatever the builder is working on.
        if (MapperHelper.isBuildWorld(world)) {
            return Optional.empty();
        }

        final List<Region> authored = new ArrayList<>();
        MapperHelper.readRegions(world).ifPresent(authored::addAll);

        final List<Region> regions = new ArrayList<>(authored);
        for (RegionContributor contributor : contributors) {
            try {
                regions.addAll(contributor.contribute(world, authored));
            } catch (Exception exception) {
                log.error("Region contributor {} failed for '{}'",
                        contributor.getClass().getSimpleName(), world.getName(), exception).submit();
            }
        }

        return regions.isEmpty() ? Optional.empty() : Optional.of(RegionIndex.of(world, regions));
    }

    /** What one world currently owns, so it can be handed back exactly. */
    private static final class WorldState {
        private final List<SceneObject> objects = new ArrayList<>();
        private final List<Zone> zones = new ArrayList<>();
    }
}
