package me.mykindos.betterpvp.core.world.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Installs {@link WorldContent} into worlds as they come and go.
 * <p>
 * Content declares which worlds it belongs to through a {@link WorldSelector}, so the same content can be authored once
 * and appear in every world that matches, including worlds that did not exist when it was declared.
 * <p>
 * Everything is scoped to one binding in one world. A world that appears is built, a world that goes away has exactly
 * its own content removed, and reloading one plugin's content leaves every other plugin's content standing.
 *
 * @see WorldContentListener for what drives it
 */
@CustomLog
@Singleton
public class WorldContentService {

    private final ZoneManager zoneManager;
    private final SceneObjectRegistry sceneRegistry;

    private final Map<WorldContentBinding, BPvPPlugin> bindings = new LinkedHashMap<>();
    private final List<RegionContributor> contributors = new ArrayList<>();
    private final Map<String, Map<WorldContentBinding, WorldContentScope>> loaded = new HashMap<>();

    /** Each world's index for as long as it is loaded, so contributors run once per load and not once per binding. */
    private final Map<String, RegionIndex> indexes = new HashMap<>();

    /** Until the first sweep, worlds are ignored. Mapper and the content declaring itself are not ready yet. */
    private boolean started;
    private boolean modelsReady;

    @Inject
    public WorldContentService(@NotNull ZoneManager zoneManager, @NotNull SceneObjectRegistry sceneRegistry) {
        this.zoneManager = zoneManager;
        this.sceneRegistry = sceneRegistry;
    }

    /**
     * Declares content and the worlds it belongs to, owned by {@code owner}: reloading that plugin re-applies it and
     * disabling it removes it. Registering after the server has started applies it immediately to every world already
     * loaded, so content that is wired up late is not silently missing until the next reload.
     */
    public void register(@NotNull BPvPPlugin owner, @NotNull WorldContentBinding binding) {
        if (!bindings.containsValue(owner)) {
            owner.getReloadables().add(() -> reload(owner));
        }

        bindings.put(binding, owner);
        Bukkit.getWorlds().forEach(world -> apply(binding, world));
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
     * Lets content that wears models in, and applies it again wherever it already stands, since a model reload
     * invalidates what was spawned against the previous registry.
     */
    public void modelsReady() {
        modelsReady = true;
        new ArrayList<>(bindings.keySet()).stream()
                .filter(WorldContentBinding::isRequiresModels)
                .forEach(this::reapply);
    }

    /**
     * Rebuilds one world's content from scratch. Safe to call on a world that is already built, since its previous
     * content is removed first, which is what lets the world-load event and the start-up sweep both fire without
     * doubling up.
     */
    public void loadWorld(@NotNull World world) {
        unloadWorld(world.getName());
        if (!started) {
            return;
        }

        final List<WorldContentBinding> matching = bindings.keySet().stream().filter(this::isReady)
                .filter(binding -> binding.matches(world)).toList();
        if (matching.isEmpty()) {
            return;
        }

        indexFor(world).ifPresent(regions -> matching.forEach(binding -> install(binding, world, regions)));
    }

    /** Removes everything this service put into {@code world}, leaving other worlds untouched. */
    public void unloadWorld(@NotNull World world) {
        unloadWorld(world.getName());
    }

    /** Re-reads the Mapper data of every world {@code owner} has content in, and re-applies all of it. */
    public void reload(@NotNull BPvPPlugin owner) {
        final List<WorldContentBinding> owned = ownedBy(owner);
        owned.forEach(binding -> binding.getOnReload().run());
        Bukkit.getWorlds().stream()
                .filter(world -> owned.stream().anyMatch(binding -> binding.matches(world)))
                .forEach(world -> indexes.remove(world.getName()));
        owned.forEach(this::reapply);
    }

    /** Removes everything {@code owner} registered from every world and forgets it, for a plugin that is going away. */
    public void release(@NotNull BPvPPlugin owner) {
        ownedBy(owner).forEach(binding -> {
            releaseEverywhere(binding);
            bindings.remove(binding);
        });
    }

    private @NotNull List<WorldContentBinding> ownedBy(@NotNull BPvPPlugin owner) {
        return bindings.entrySet().stream().filter(entry -> entry.getValue() == owner).map(Map.Entry::getKey).toList();
    }

    private boolean isReady(@NotNull WorldContentBinding binding) {
        return started && (modelsReady || !binding.isRequiresModels());
    }

    private void reapply(@NotNull WorldContentBinding binding) {
        releaseEverywhere(binding);
        Bukkit.getWorlds().forEach(world -> apply(binding, world));
    }

    private void releaseEverywhere(@NotNull WorldContentBinding binding) {
        loaded.values().forEach(scopes -> {
            final WorldContentScope scope = scopes.remove(binding);
            if (scope != null) {
                scope.release();
            }
        });
    }

    private void apply(@NotNull WorldContentBinding binding, @NotNull World world) {
        if (isReady(binding) && binding.matches(world)) {
            indexFor(world).ifPresent(regions -> install(binding, world, regions));
        }
    }

    // By name, not by World: a world may already be gone from Bukkit, and its objects still need unregistering.
    private void unloadWorld(@NotNull String worldName) {
        indexes.remove(worldName);
        final Map<WorldContentBinding, WorldContentScope> scopes = loaded.remove(worldName);
        if (scopes != null) {
            scopes.values().forEach(WorldContentScope::release);
        }
    }

    /**
     * Installs one binding's content into one world. Each piece of content is isolated, and so is each scene object,
     * because one bad data-point should cost its own content, not the rest of the world's.
     */
    private void install(@NotNull WorldContentBinding binding, @NotNull World world, @NotNull RegionIndex regions) {
        final WorldContentScope scope = new WorldContentScope(zoneManager, sceneRegistry);
        final WorldContentScope previous = loaded.computeIfAbsent(world.getName(), name -> new LinkedHashMap<>())
                .put(binding, scope);
        if (previous != null) {
            previous.release();
        }

        for (WorldContent content : binding.getContent().get()) {
            try {
                for (Zone zone : content.zones(world, regions)) {
                    scope.add(zone);
                }
                for (SceneSpawn spawn : content.sceneObjects(world, regions)) {
                    try {
                        scope.add(spawn);
                    } catch (Exception exception) {
                        log.error("Scene object from {} failed to load at {}",
                                content.getClass().getSimpleName(), spawn.getAnchor(), exception).submit();
                    }
                }
                content.install(world, regions, scope);
            } catch (Exception exception) {
                log.error("Content {} failed to load into '{}'",
                        content.getClass().getSimpleName(), world.getName(), exception).submit();
            }
        }

        if (scope.objectCount() > 0 || scope.zoneCount() > 0) {
            log.info("Loaded {} scene object(s) and {} zone(s) into '{}' for {}", scope.objectCount(),
                    scope.zoneCount(), world.getName(), bindings.get(binding).getName()).submit();
        }
    }

    /**
     * Builds the world's data-point index: what is authored on disk, plus whatever the contributors put there this
     * load. A world with no Mapper file gets an empty index, since content such as player-placed objects does not come
     * from Mapper at all.
     */
    private Optional<RegionIndex> indexFor(@NotNull World world) {
        final RegionIndex known = indexes.get(world.getName());
        if (known != null) {
            return Optional.of(known);
        }

        // A map still being authored gets nothing at all, not even the contributors, whose structures would otherwise
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

        final RegionIndex index = RegionIndex.of(world, regions);
        indexes.put(world.getName(), index);
        return Optional.of(index);
    }
}
