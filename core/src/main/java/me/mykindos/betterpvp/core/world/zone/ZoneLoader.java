package me.mykindos.betterpvp.core.world.zone;

import me.mykindos.betterpvp.core.scene.loader.TrackedLoader;
import org.jetbrains.annotations.NotNull;

/**
 * {@link TrackedLoader} specialisation for {@link Zone}s sourced from an external data source (Mapper data-points,
 * config, ...). Subclasses build their zones inside {@link #load()} and register each one via {@link #register(Zone)};
 * the base class unregisters them all from the {@link ZoneManager} on the next {@link #reload()}/shutdown.
 * <p>
 * Reuses the same {@link me.mykindos.betterpvp.core.scene.loader.LoadStrategy strategy} and
 * {@link me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager manager} plumbing as scene-object loaders, so a
 * zone loader can declare {@code ServerStartLoadStrategy}/{@code ModuleReloadLoadStrategy} just like a
 * {@link me.mykindos.betterpvp.core.scene.loader.MapperSceneLoader} does.
 */
public abstract class ZoneLoader extends TrackedLoader<Zone> {

    private final ZoneManager zoneManager;

    protected ZoneLoader(@NotNull ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @Override
    protected void releaseTracked(@NotNull Zone zone) {
        zoneManager.unregister(zone);
    }

    /**
     * Registers {@code zone} with the {@link ZoneManager} and tracks it so it is unregistered on the next reload.
     *
     * @param zone the zone to register
     * @return the same zone, now registered and tracked
     */
    protected Zone register(@NotNull Zone zone) {
        zoneManager.register(zone);
        return track(zone);
    }

    protected @NotNull ZoneManager getZoneManager() {
        return zoneManager;
    }

}
