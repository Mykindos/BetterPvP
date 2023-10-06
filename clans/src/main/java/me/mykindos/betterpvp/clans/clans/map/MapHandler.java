package me.mykindos.betterpvp.clans.clans.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapPixel;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.renderer.ClanMapRenderer;
import me.mykindos.betterpvp.clans.clans.map.renderer.MinimapRenderer;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
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


/**
 * Map system by <a href="https://github.com/areeoh/">Areeoh</a>
 * Modified by Tom Hoogstra for 1.19
 */
@Slf4j
@Singleton
public class MapHandler {

    @Inject
    @Config(path = "clans.map.enabled", defaultValue = "true")
    public boolean enabled;

    @Inject
    @Config(path = "clans.map.update-interval", defaultValue = "1")
    public int updateInterval;

    private final Clans clans;
    public final HashMap<UUID, Set<ChunkData>> clanMapData = new HashMap<>();
    public final Map<UUID, MapSettings> mapSettingsMap = new HashMap<>();

    @Inject
    public MapHandler(Clans clans) {
        this.clans = clans;
        UtilServer.runTaskTimerAsync(clans, this::saveMapData, 6000, 6000);
    }

    public boolean hasMoved(Player player) {
        final MapSettings mapData = mapSettingsMap.get(player.getUniqueId());
        if (mapData == null) return false;


        int distX = Math.abs(mapData.getMapX() - player.getLocation().getBlockX());
        int distZ = Math.abs(mapData.getMapZ() - player.getLocation().getBlockZ());
        final int scale = 1 << mapData.getScale().getValue();
        return (distX >= scale) || (distZ >= scale);
    }

    public void updateLastMoved(Player player) {
        final MapSettings mapData = mapSettingsMap.get(player.getUniqueId());
        if (mapData == null) return;

        mapData.setMapX(player.getLocation().getBlockX());
        mapData.setMapZ(player.getLocation().getBlockZ());
    }


    public synchronized void loadMap() {

        try {

            File file = new File("./world/data/map_0.dat");
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    log.error("Failed to create blank map file");
                }
            }

            World world = Bukkit.getWorld("world");
            if (world == null) {
                log.error("Could not load map as main world does not exist");
                return;
            }

            MapView map = Bukkit.getMap(0);
            if (map == null) {
                map = Bukkit.createMap(world);
            }
            if (!(map.getRenderers().get(0) instanceof MinimapRenderer)) {
                for (MapRenderer r : map.getRenderers()) {
                    map.removeRenderer(r);
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
            loadMapData((MinimapRenderer) map.getRenderers().get(0));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadMapData(MinimapRenderer minimapRenderer) {
        // Load async as well, just to let server boot up quicker.
        UtilServer.runTaskAsync(clans, () -> {
            final long l = System.currentTimeMillis();

            final File file = new File("world/data/map.json");

            if (!file.exists()) {
                return;
            }

            JSONParser parser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(file));

                jsonObject.forEach((key, value) -> {
                    minimapRenderer.getWorldCacheMap().put(key.toString(), new HashMap<>());
                    ((JSONObject) value).forEach((o, o2) -> {
                        minimapRenderer.getWorldCacheMap().get(key.toString()).put(Integer.parseInt((String) o), new HashMap<>());
                        ((JSONObject) o2).forEach((o1, o21) -> {
                            JSONObject jsonObject1 = (JSONObject) o21;
                            minimapRenderer.getWorldCacheMap().get(key.toString()).get(Integer.parseInt(String.valueOf(o))).put(Integer.parseInt((String) o1),
                                    new MapPixel((int) (long) jsonObject1.get("colorId"), (short) (long) jsonObject1.get("averageY")));
                        });
                    });
                });
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
            log.info("Loaded map data in {}", UtilTime.getTime(System.currentTimeMillis() - l, UtilTime.TimeUnit.SECONDS, 2));
        });
    }

    public void saveMapData() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final long l = System.currentTimeMillis();

                log.info("Saving map data...");

                MapView map = Bukkit.getMap(0);
                if (map == null) {
                    map = Bukkit.createMap(Bukkit.getWorld("world"));
                }
                MinimapRenderer minimapRenderer = (MinimapRenderer) map.getRenderers().get(0);

                try {
                    final File file = new File("world/data/map.json");
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(file, minimapRenderer.getWorldCacheMap());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("Saved map data in {}", UtilTime.getTime(System.currentTimeMillis() - l, UtilTime.TimeUnit.SECONDS, 2));
            }
        }.runTaskAsynchronously(clans);
    }

}
