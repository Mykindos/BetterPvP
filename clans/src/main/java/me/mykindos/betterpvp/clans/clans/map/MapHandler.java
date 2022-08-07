package me.mykindos.betterpvp.clans.clans.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapPixel;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.renderer.MinimapRenderer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.minecraft.world.level.material.MaterialColor;
import org.bukkit.Bukkit;
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
import java.util.*;

@Singleton
public class MapHandler {

    private final Clans clans;

    public HashMap<UUID, Set<ChunkData>> clanMapData = new HashMap<>();
    public Map<UUID, MapSettings> mapSettingsMap = new HashMap<>();

    @Inject
    public MapHandler(Clans clans) {
        this.clans = clans;

        UtilServer.runTaskTimerAsync(clans, this::saveMapData, 6000, 6000);
    }

    public boolean hasMoved(Player player) {
        if (!mapSettingsMap.containsKey(player.getUniqueId())) {
            return false;
        }
        final MapSettings mapData = mapSettingsMap.get(player.getUniqueId());
        int distX = Math.abs(mapData.getMapX() - player.getLocation().getBlockX());
        int distZ = Math.abs(mapData.getMapZ() - player.getLocation().getBlockZ());
        final int scale = 1 << mapData.getScale().getValue();
        return (distX >= scale) || (distZ >= scale);
    }

    public synchronized void loadMap() {

        try {
            MapView map = Bukkit.getMap(0);
            if (map == null) {
                map = Bukkit.createMap(Bukkit.getWorld("world"));
            }
            if (!(map.getRenderers().get(0) instanceof MinimapRenderer)) {
                for (final MapRenderer r : map.getRenderers()) {
                    map.removeRenderer(r);
                }
                final MinimapRenderer renderer = new MinimapRenderer(this, clans);
                map.addRenderer(renderer);
                // TODO
                //map.addRenderer(new ClanMapRenderer(this));
            }
            loadMapData((MinimapRenderer) map.getRenderers().get(0));
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void deleteFolder(File folder) {
        if(!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }


    public synchronized void loadMapData(MinimapRenderer minimapRenderer) {
        final long l = System.currentTimeMillis();

        final File file = new File(clans.getDataFolder().getPath(), "map.json");

        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(file));

            jsonObject.forEach((key, value) -> {
                Bukkit.broadcastMessage(key + "");
                minimapRenderer.getWorldCacheMap().put(key.toString(), new TreeMap<>());
                ((JSONObject) value).forEach((o, o2) -> {
                    minimapRenderer.getWorldCacheMap().get(key.toString()).put(Integer.parseInt((String) o), new TreeMap<>());
                    ((JSONObject) o2).forEach((o1, o21) -> {
                        JSONObject jsonObject1 = (JSONObject) o21;
                        minimapRenderer.getWorldCacheMap().get(key.toString()).get(Integer.parseInt((String) o)).put(Integer.parseInt((String) o1),
                                new MapPixel((MaterialColor) jsonObject1.get("color"), (short) (long) jsonObject1.get("averageY")));
                    });
                });
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Loaded map data in " + UtilTime.getTime(System.currentTimeMillis() - l, UtilTime.TimeUnit.SECONDS, 2));
    }

    public void saveMapData() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final long l = System.currentTimeMillis();

                System.out.println("Saving map data...");

                MapView map = Bukkit.getMap(0);
                if (map == null) {
                    map = Bukkit.createMap(Bukkit.getWorld("world"));
                }
                MinimapRenderer minimapRenderer = (MinimapRenderer) map.getRenderers().get(0);

                try {
                    final File file = new File(clans.getDataFolder().getPath(), "map.json");
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(file, minimapRenderer.getWorldCacheMap());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Saved map data in " + UtilTime.getTime(System.currentTimeMillis() - l, UtilTime.TimeUnit.SECONDS, 2));
            }
        }.runTaskAsynchronously(clans);
    }

}
