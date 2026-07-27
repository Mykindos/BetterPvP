package me.mykindos.betterpvp.clans.world;

import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader;
import me.mykindos.betterpvp.core.scene.loader.ServerStartLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.WorldLoadStrategy;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Collection;
import java.util.List;

/**
 * The generic scene loader for a {@link Island}: on each (re)load it spawns every {@link SceneSpawn} its island's
 * content contributes, registering each with the {@link SceneObjectRegistry} so behaviours tick. It is a plain
 * {@link SceneObjectLoader}, so init/register/remove and the tracked reload lifecycle are inherited, not reimplemented.
 * One instance exists per island, created by {@link Island}.
 */
@CustomLog
public class IslandSceneLoader extends SceneObjectLoader {

    private final SceneObjectRegistry registry;
    private final Island island;

    public IslandSceneLoader(SceneObjectRegistry registry, Island island) {
        this.registry = registry;
        this.island = island;
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        // WorldLoadStrategy covers islands whose world loads after server-start (e.g. on-demand worlds): without it
        // the one-shot server-start pass runs before the world exists and silently finds nothing.
        return List.of(new ServerStartLoadStrategy(), new WorldLoadStrategy(island.worldName()), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void load() {
        final World world = Bukkit.getWorld(island.worldName());
        if (world == null) {
            log.warn("Cannot load {} scene objects: world '{}' is not loaded", island.name(), island.worldName()).submit();
            return;
        }

        final Collection<Region> regions = MapperHelper.getRegions(world);
        int count = 0;
        for (WorldContent content : island.content()) {
            for (SceneSpawn spawn : content.sceneObjects(world, regions)) {
                // Register chunk-managed: the body is spawned by entityFactory when the anchor chunk loads, not now.
                spawn(spawn.getObject(), spawn.getAnchor(), spawn.getEntityFactory(), registry);
                count++;
            }
        }
        log.info("Loaded {} scene objects for {}", count, island.name()).submit();
    }
}
