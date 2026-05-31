package me.mykindos.betterpvp.clans.world;

import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.world.zone.Zone;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * A self-contained piece of world-expansion content (a gateway, an arena, a town, ...).
 * <p>
 * It contributes its capability {@link Zone zones} and its {@link SceneSpawn scene objects} from Mapper data — that's
 * all. A {@link Continent} bundles content and hands each slice to the matching generic loader
 * ({@link ContinentZoneLoader} / {@link ContinentSceneLoader}), so content owns <em>what</em> it is while the loaders
 * own registration and teardown. Implement only the method(s) relevant to the content; both default to empty.
 */
public interface WorldContent {

    /**
     * @param world   the world being loaded
     * @param regions all Mapper regions in {@code world}
     * @return the capability zones this content registers (empty if none)
     */
    default @NotNull List<Zone> zones(@NotNull World world, @NotNull Collection<Region> regions) {
        return List.of();
    }

    /**
     * @param world   the world being loaded
     * @param regions all Mapper regions in {@code world}
     * @return the scene objects this content spawns, each paired with its backing entity (empty if none)
     */
    default @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull Collection<Region> regions) {
        return List.of();
    }
}
