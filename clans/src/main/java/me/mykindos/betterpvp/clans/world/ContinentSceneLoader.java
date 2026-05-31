package me.mykindos.betterpvp.clans.world;

import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneObjectLoader;
import me.mykindos.betterpvp.core.scene.loader.ServerStartLoadStrategy;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Collection;
import java.util.List;

/**
 * The generic scene loader for a {@link Continent}: on each (re)load it spawns every {@link SceneSpawn} its continent's
 * content contributes, registering each with the {@link SceneObjectRegistry} so behaviours tick. It is a plain
 * {@link SceneObjectLoader}, so init/register/remove and the tracked reload lifecycle are inherited, not reimplemented.
 * One instance exists per continent, created by {@link Continent}.
 */
@CustomLog
public class ContinentSceneLoader extends SceneObjectLoader {

    private final SceneObjectRegistry registry;
    private final Continent continent;

    public ContinentSceneLoader(SceneObjectRegistry registry, Continent continent) {
        this.registry = registry;
        this.continent = continent;
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        return List.of(new ServerStartLoadStrategy(), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void load() {
        final World world = Bukkit.getWorld(continent.worldName());
        if (world == null) {
            log.warn("Cannot load {} scene objects: world '{}' is not loaded", continent.name(), continent.worldName()).submit();
            return;
        }

        final Collection<Region> regions = MapperHelper.getRegions(world);
        for (WorldContent content : continent.content()) {
            for (SceneSpawn spawn : content.sceneObjects(world, regions)) {
                spawn(spawn.getObject(), spawn.getEntity(), registry);
            }
        }
    }
}
