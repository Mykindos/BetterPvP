package me.mykindos.betterpvp.core.world.content;

import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.Zone;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A self-contained piece of world content (a gateway, a dock, a town's residents, a set of props).
 * <p>
 * It contributes its capability {@link Zone zones} and its {@link SceneSpawn scene objects} from Mapper data. The
 * {@link WorldContentService} decides which worlds it belongs to and owns registration and teardown, so content owns
 * <em>what</em> it is and never <em>where</em> or <em>when</em>. Implement only the method(s) relevant to the content,
 * all of them default to nothing.
 * <p>
 * Content is asked about one world at a time and must keep no state about the world it was last asked about: the same
 * instance is used for every world it matches, including instanced islands cloned from a single template.
 */
public interface WorldContent {

    /**
     * @param world   the world being loaded
     * @param regions that world's Mapper regions, indexed by data-point name and already bound to the world
     * @return the capability zones this content registers (empty if none)
     */
    default @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        return List.of();
    }

    /**
     * @param world   the world being loaded
     * @param regions that world's Mapper regions, indexed by data-point name and already bound to the world
     * @return the chunk-managed scene objects this content spawns, each paired with its backing entity (empty if none)
     */
    default @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull RegionIndex regions) {
        return List.of();
    }

    /**
     * For content that does not fit {@link #zones} or {@link #sceneObjects}: objects that are spawned eagerly, things
     * that need their own teardown, or content that adds to the world after it has loaded. Whatever is handed to the
     * scope is released with this world's content.
     *
     * @param world   the world being loaded
     * @param regions that world's Mapper regions, indexed by data-point name and already bound to the world
     * @param scope   what this content owns in the world, which it may keep and add to until the world is released
     */
    default void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
    }
}
