package me.mykindos.betterpvp.clans.clans.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.claim.ClanClaimIndex;
import me.mykindos.betterpvp.clans.clans.map.cursor.MapCursorService;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.frame.MapFrameService;
import me.mykindos.betterpvp.clans.clans.map.renderer.BPvPMapRenderer;
import me.mykindos.betterpvp.clans.clans.map.terrain.MapTerrainService;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point and per-viewer state for the map.
 * <p>
 * Map system by <a href="https://github.com/areeoh/">Areeoh</a>, modified by Mykindos for 1.19+. Drawing belongs to
 * the terrain, frame and cursor services, so this class owns each viewer's settings and the wiring that starts them.
 */
@CustomLog
@Singleton
public class MapHandler {

    private final Clans clans;

    @Inject
    @Config(path = "clans.map.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "clans.map.update-interval", defaultValue = "1")
    private int updateInterval;

    @Getter
    private final Map<UUID, MapSettings> mapSettingsMap = new ConcurrentHashMap<>();

    @Inject
    public MapHandler(Clans clans) {
        this.clans = clans;
        // A tick's delay so every world is loaded before the shared map view is bound and terrain begins loading.
        UtilServer.runTaskLater(clans, this::loadMap, 1L);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public @NotNull MapSettings getOrCreateMapSettings(@NotNull Player player) {
        return mapSettingsMap.computeIfAbsent(player.getUniqueId(),
                ignored -> new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
    }

    /**
     * Binds the shared map view to the single renderer and starts the terrain, frame and cursor services.
     */
    public synchronized void loadMap() {
        try {
            final World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
            if (world == null) {
                log.error("Could not load map as main world does not exist").submit();
                return;
            }

            final MapView map = getOrCreateMapView(world);
            if (map.getRenderers().isEmpty() || !(map.getRenderers().getFirst() instanceof BPvPMapRenderer)) {
                for (MapRenderer renderer : map.getRenderers()) {
                    map.removeRenderer(renderer);
                }
                map.addRenderer(clans.getInjector().getInstance(BPvPMapRenderer.class));
            }

            // Pulled from the injector rather than constructor-injected, so these services can depend on this one.
            clans.getInjector().getInstance(MapCursorService.class);
            clans.getInjector().getInstance(MapFrameService.class);
            clans.getInjector().getInstance(ClanClaimIndex.class).rebuild();
            clans.getInjector().getInstance(MapTerrainService.class).prepare(world);
        } catch (Exception exception) {
            log.error("Failed to load map", exception).submit();
        }
    }

    private MapView getOrCreateMapView(World world) {
        final MapView map = Bukkit.getMap(0);
        return map == null ? Bukkit.createMap(world) : map;
    }

    /**
     * Schedules a save of every mapped world's terrain.
     */
    public void saveMapData() {
        clans.getInjector().getInstance(MapTerrainService.class).saveAll();
    }

    /**
     * Writes terrain on the calling thread and stops the map's worker threads. For shutdown, where scheduled tasks no
     * longer run reliably.
     */
    public void saveMapDataNow() {
        clans.getInjector().getInstance(MapFrameService.class).shutdown();
        clans.getInjector().getInstance(MapTerrainService.class).shutdown();
    }

    /**
     * Discards a world's terrain, on disk and in memory, and forces every viewer to redraw.
     */
    public void resetMapData(@NotNull World world) {
        clans.getInjector().getInstance(MapTerrainService.class).reset(world);
        forceRedrawAll();
    }

    public void forceRedrawAll() {
        mapSettingsMap.values().forEach(settings -> settings.setForceRedraw(true));
    }
}
