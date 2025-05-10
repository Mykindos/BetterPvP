package me.mykindos.betterpvp.clans.clans.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapPixel;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.renderer.ClanMapRenderer;
import me.mykindos.betterpvp.clans.clans.map.renderer.MinimapRenderer;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map system by <a href="https://github.com/areeoh/">Areeoh</a>
 * Modified by Mykindos for 1.19+
 */
@CustomLog
@Singleton
public class MapHandler {
    private static final String MAP_DATA_FILENAME = "map.json";
    private static final String MAP_FILE_FILENAME = "map_0.dat";
    private static final int SAVE_INTERVAL_TICKS = 6000;

    @Inject
    @Config(path = "clans.map.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "clans.map.update-interval", defaultValue = "1")
    private int updateInterval;

    private final Clans clans;

    @Getter
    private final Map<UUID, Set<ChunkData>> clanMapData = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, MapSettings> mapSettingsMap = new ConcurrentHashMap<>();

    @Inject
    public MapHandler(Clans clans) {
        this.clans = clans;
        UtilServer.runTaskTimerAsync(clans, this::saveMapData, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    /**
     * Determines if a player has moved enough to trigger a map update
     *
     * @param player The player to check
     * @return True if the player has moved enough to update the map
     */
    public boolean hasMoved(Player player) {
        MapSettings mapSettings = getOrCreateMapSettings(player);

        int distX = Math.abs(mapSettings.getMapX() - player.getLocation().getBlockX());
        int distZ = Math.abs(mapSettings.getMapZ() - player.getLocation().getBlockZ());
        int scale = mapSettings.getScale().getValue();

        return (distX >= scale) || (distZ >= scale);
    }

    /**
     * Updates the player's last moved position
     *
     * @param player The player whose position to update
     */
    public void updateLastMoved(Player player) {
        MapSettings mapSettings = getOrCreateMapSettings(player);
        mapSettings.setMapX(player.getLocation().getBlockX());
        mapSettings.setMapZ(player.getLocation().getBlockZ());
    }

    private MapSettings getOrCreateMapSettings(Player player) {
        return mapSettingsMap.computeIfAbsent(player.getUniqueId(),
                k -> new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ()));
    }

    /**
     * Loads the map and initializes renderers
     */
    public synchronized void loadMap() {
        try {
            World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
            if (world == null) {
                log.error("Could not load map as main world does not exist").submit();
                return;
            }

            createFileIfNotExists(getMapDataFile(MAP_FILE_FILENAME));

            MapView map = getOrCreateMapView(world);
            if (!(map.getRenderers().getFirst() instanceof MinimapRenderer)) {
                initializeRenderers(map);
            }

            loadMapData((MinimapRenderer) map.getRenderers().getFirst());
        } catch (Exception ex) {
            log.error("Failed to load map", ex).submit();
        }
    }

    private MapView getOrCreateMapView(World world) {
        MapView map = Bukkit.getMap(0);
        if (map == null) {
            map = Bukkit.createMap(world);
        }
        return map;
    }

    private void initializeRenderers(MapView map) {
        for (MapRenderer renderer : map.getRenderers()) {
            map.removeRenderer(renderer);
        }

        MinimapRenderer minimapRenderer = clans.getInjector().getInstance(MinimapRenderer.class);
        ClanMapRenderer clanMapRenderer = clans.getInjector().getInstance(ClanMapRenderer.class);

        clans.getInjector().injectMembers(minimapRenderer);
        clans.getInjector().injectMembers(clanMapRenderer);

        clans.getListeners().add(minimapRenderer);
        clans.saveConfig();

        map.addRenderer(minimapRenderer);
        map.addRenderer(clanMapRenderer);
    }

    /**
     * Loads map data from disk into the renderer
     *
     * @param minimapRenderer The renderer to load data into
     */
    @SuppressWarnings("unchecked")
    public void loadMapData(MinimapRenderer minimapRenderer) {
        // Load async to let server boot up quicker
        UtilServer.runTaskAsync(clans, () -> {
            final long startTime = System.currentTimeMillis();
            File mapDataFile = getMapDataFile(MAP_DATA_FILENAME);

            if (!mapDataFile.exists()) {
                return;
            }

            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(mapDataFile));
                parseMapData(jsonObject, minimapRenderer);

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("Loaded map data in {}", UtilTime.getTime(elapsed, 2)).submit();
            } catch (IOException | ParseException e) {
                log.error("Failed to load map data", e).submit();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void parseMapData(JSONObject jsonObject, MinimapRenderer minimapRenderer) {
        jsonObject.forEach((key, value) -> {
            String worldName = key.toString();
            minimapRenderer.getWorldCacheMap().put(worldName, new HashMap<>());

            ((JSONObject) value).forEach((xStr, xValue) -> {
                int x = Integer.parseInt((String) xStr);
                minimapRenderer.getWorldCacheMap().get(worldName).put(x, new HashMap<>());

                ((JSONObject) xValue).forEach((zStr, zValue) -> {
                    int z = Integer.parseInt((String) zStr);
                    JSONObject pixelData = (JSONObject) zValue;

                    int colorId = (int) (long) pixelData.get("colorId");
                    short averageY = (short) (long) pixelData.get("averageY");

                    minimapRenderer.getWorldCacheMap().get(worldName).get(x)
                            .put(z, new MapPixel(colorId, averageY));
                });
            });
        });
    }

    /**
     * Saves the map data to disk
     */
    @SuppressWarnings("DataFlowIssue")
    public void saveMapData() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                log.info("Saving map data...").submit();

                MapView map = getOrCreateMapView(Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME));

                if (map.getRenderers().getFirst() instanceof MinimapRenderer minimapRenderer) {
                    try {
                        File mapDataFile = getMapDataFile(MAP_DATA_FILENAME);
                        createFileIfNotExists(mapDataFile);

                        ObjectMapper mapper = new ObjectMapper();
                        mapper.writeValue(mapDataFile, minimapRenderer.getWorldCacheMap());

                        long elapsed = System.currentTimeMillis() - startTime;
                        log.info("Saved map data in {}", UtilTime.getTime(elapsed, 2)).submit();
                    } catch (IOException e) {
                        log.error("Failed to save map data", e).submit();
                    }
                }
            }
        }.runTaskAsynchronously(clans);
    }

    /**
     * Resets all map data
     */
    public void resetMapData() {
        File mapDataFile = getMapDataFile(MAP_DATA_FILENAME);

        if (!mapDataFile.exists()) {
            return;
        }

        if (!mapDataFile.delete()) {
            log.error("Failed to delete map data file").submit();
        }

        clans.getInjector().getInstance(MinimapRenderer.class).getWorldCacheMap().clear();
    }

    private File getMapDataFile(String filename) {
        return new File(Bukkit.getWorldContainer(), BPvPWorld.MAIN_WORLD_NAME + "/data/" + filename);
    }

    private void createFileIfNotExists(File file) throws IOException {
        if (!file.exists() && !file.createNewFile()) {
            log.error("Failed to create file: {}", file.getName()).submit();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }
}