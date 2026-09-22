package me.mykindos.betterpvp.core.world.content;

import dev.brauw.mapper.region.PerspectiveRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Spawns one of a factory's objects at every perspective data-point named {@code <prefix>:<type>}, for example
 * {@code shops:fisherman}, the same way {@code /npc spawn <prefix> <type>} would.
 */
@CustomLog
public final class FactorySpawnPoints implements WorldContent {

    private final String marker;
    private final SceneObjectFactory factory;

    public FactorySpawnPoints(@NotNull String prefix, @NotNull SceneObjectFactory factory) {
        this.marker = prefix + ":";
        this.factory = factory;
    }

    @Override
    public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
        for (Region region : regions.all()) {
            if (!(region instanceof PerspectiveRegion point) || !region.getName().startsWith(marker)) {
                continue;
            }

            point.setWorld(world);
            final String type = region.getName().substring(marker.length()).toLowerCase(Locale.ROOT);
            try {
                final SceneObject object = factory.spawnDefault(point.getLocation(), type);
                if (object != null) {
                    scope.adopt(object);
                }
            } catch (Exception exception) {
                log.error("Could not spawn '{}' from data-point '{}' in '{}'", type, region.getName(), world.getName(),
                        exception).submit();
            }
        }
    }
}
