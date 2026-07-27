package me.mykindos.betterpvp.clans.world;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A bundle of {@link WorldContent} for one island.
 * <p>
 * On construction it spins up the two generic loaders ({@link IslandZoneLoader}, {@link IslandSceneLoader}) and
 * registers them with the {@link SceneLoaderManager}. Those loaders pull this island's {@link #content()} at
 * (re)load time and delegate to the standard zone/scene loader lifecycle — so an island never contains spawning
 * logic, zone-building, or prop/NPC factories. Subclasses declare only their content.
 * <p>
 * {@link #content()} is read lazily (at load time, not construction), so subclasses may set up their content fields
 * after calling {@code super(...)}.
 */
public abstract class Island {

    protected Island(@NotNull ZoneManager zoneManager, @NotNull SceneObjectRegistry sceneRegistry,
                        @NotNull SceneLoaderManager loaderManager, @NotNull Clans clans) {
        loaderManager.register(new IslandZoneLoader(zoneManager, this), clans);
        loaderManager.register(new IslandSceneLoader(sceneRegistry, this), clans);
    }

    /**
     * @return all content on this island. Adding content is a one-liner here.
     */
    public abstract @NotNull List<WorldContent> content();

    /**
     * @return the world this island lives in; defaults to the main world
     */
    public @NotNull String worldName() {
        return BPvPWorld.MAIN_WORLD_NAME;
    }

    /**
     * @return a short name for logging
     */
    public abstract @NotNull String name();

}
