package me.mykindos.betterpvp.clans.world;

import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ServerStartLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.WorldLoadStrategy;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.zone.ZoneLoader;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Collection;
import java.util.List;

/**
 * The generic zone loader for a {@link Island}: on each (re)load it registers every {@link #register zone} its
 * island's content contributes. It is a plain {@link ZoneLoader}, so register/unregister and the tracked reload
 * lifecycle are inherited, not reimplemented. One instance exists per island, created by {@link Island}.
 */
@CustomLog
public class IslandZoneLoader extends ZoneLoader {

    private final Island island;

    public IslandZoneLoader(ZoneManager zoneManager, Island island) {
        super(zoneManager);
        this.island = island;
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        // WorldLoadStrategy ensures the island's zones register even when its world loads after server-start.
        return List.of(new ServerStartLoadStrategy(), new WorldLoadStrategy(island.worldName()), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void load() {
        final World world = Bukkit.getWorld(island.worldName());
        if (world == null) {
            log.warn("Cannot load {} zones: world '{}' is not loaded", island.name(), island.worldName()).submit();
            return;
        }

        final Collection<Region> regions = MapperHelper.getRegions(world);
        for (WorldContent content : island.content()) {
            content.zones(world, regions).forEach(this::register);
        }
    }
}
