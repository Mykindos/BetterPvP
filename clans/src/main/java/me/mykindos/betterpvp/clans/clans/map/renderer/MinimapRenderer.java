package me.mykindos.betterpvp.clans.clans.map.renderer;

import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.Coords;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.data.MapPixel;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.clans.clans.map.nms.UtilMapMaterial;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

@Getter
public class MinimapRenderer extends MapRenderer implements Listener {

    private final MapHandler mapHandler;
    protected Map<String, Map<Integer, Map<Integer, MapPixel>>> worldCacheMap = new TreeMap<>();
    protected Queue<Coords> queue = new LinkedList<>();

    public MinimapRenderer(MapHandler mapHandler, Clans clans){
        this.mapHandler = mapHandler;
        Bukkit.getPluginManager().registerEvents(this, clans);
    }

    private void processQueue() {
        if(queue.isEmpty()) {
            return;
        }

        for (int i = 0; i < 64; i++) {
            final Coords poll = queue.poll();

            if (poll == null) {
                break;
            }

            World world = Bukkit.getWorld(poll.getWorld());

            if (!getWorldCacheMap().containsKey(poll.getWorld())) {
                continue;
            }
            if (!getWorldCacheMap().get(poll.getWorld()).containsKey(poll.getX())) {
                continue;
            }
            if (!getWorldCacheMap().get(poll.getWorld()).get(poll.getX()).containsKey(poll.getZ())) {
                continue;
            }
            Block b = world.getBlockAt(poll.getX(), world.getHighestBlockYAt(poll.getX(), poll.getZ()), poll.getZ());

            if (!b.getChunk().isLoaded()) {
                continue;
            }
            while (b.getY() > 0 && UtilMapMaterial.getBlockColor(b) == UtilMapMaterial.getColorNeutral()) {
                b = world.getBlockAt(b.getX(), b.getY() - 1, b.getZ());
            }
            short avgY = 0;
            avgY += b.getY();

            var mainColor = UtilMapMaterial.getBlockColor(b);

            final MapPixel mapPixel = getWorldCacheMap().get(b.getWorld().getName()).get(b.getX()).get(b.getZ());
            mapPixel.setAverageY(avgY);
            mapPixel.setColor(mainColor);
        }
    }


    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();

        final MapSettings mapSettings = mapHandler.mapSettingsMap.get(player.getUniqueId());

        int scale = 1 << mapSettings.getScale().getValue();

        if (mapSettings.getScale() == MapSettings.Scale.FAR) {
            centerX = 0;
            centerZ = 0;
        }

        if (!worldCacheMap.containsKey(player.getWorld().getName()))
            worldCacheMap.put(player.getWorld().getName(), new TreeMap<>());

        final Map<Integer, Map<Integer, MapPixel>> cacheMap = worldCacheMap.get(player.getWorld().getName());

        final boolean hasMoved = mapHandler.hasMoved(player);

        if (hasMoved || mapSettings.isUpdate()) {
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    canvas.setPixel(i, j, (byte) 0);
                }
            }
            int locX = centerX / scale - 64;
            int locZ = centerZ / scale - 64;
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    int x = (locX + i) * scale;
                    int z = (locZ + j) * scale;

                    if (locX + i < 0 && (locX + i) % scale != 0)
                        x--;
                    if (locZ + j < 0 && (locZ + j) % scale != 0)
                        z--;
                    if (cacheMap.containsKey(x) && cacheMap.get(x).containsKey(z)) {
                        final MapPixel mapPixel = cacheMap.get(x).get(z);
                        short prevY = getPrevY(x, z, player.getWorld().getName(), scale);

                        double d2 = (mapPixel.getAverageY() - prevY) * 4.0D / (scale + 4) + ((i + j & 1) - 0.5D) * 0.4D;
                        byte b0 = 1;

                        if (d2 > 0.6D) {
                            b0 = 2;
                        }
                        if (d2 < -0.6D) {
                            b0 = 0;
                        }
                        canvas.setPixel(i, j, (byte) (mapPixel.getColor().col + b0));
                    } else {
                        for (int k = -scale; k < scale; k++) {
                            for (int l = -scale; l < scale; l++) {
                                handlePixel(cacheMap, x + k, z + l, player);
                            }
                        }
                    }
                }
            }
        }
        handleCursors(canvas, player, scale, centerX, centerZ);
    }

    private void addToQueue(Coords coords) {
        if(!queue.contains(coords)) {
            queue.add(coords);
        }
    }

    private void handlePixel(Map<Integer, Map<Integer, MapPixel>> cacheMap, int x, int z, Player player) {
        if (!cacheMap.containsKey(x)) {
            cacheMap.put(x, new TreeMap<>());
        }
        if (!cacheMap.get(x).containsKey(z)) {
            //IF WANT HEIGHT LIMIT JUST CHANGE THIS
            int y = player.getWorld().getHighestBlockYAt(x, z) + 1;

            Block b = player.getWorld().getBlockAt(x, y, z);

            if (!b.getChunk().isLoaded()) {
                return;
            }
            while (b.getY() > 0 && UtilMapMaterial.getBlockColor(b) == UtilMapMaterial.getColorNeutral()) {
                b = player.getWorld().getBlockAt(b.getX(), b.getY() - 1, b.getZ());
            }
            short avgY = 0;
            avgY += b.getY();

            var mainColor = UtilMapMaterial.getBlockColor(b);

            cacheMap.get(x).put(z, new MapPixel(mainColor, avgY));
        }
    }

    private void handleCursors(MapCanvas canvas, Player player, int scale, int centerX, int centerZ) {
        MapCursorCollection cursors = canvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        MinimapExtraCursorEvent e = new MinimapExtraCursorEvent(player, cursors, scale);
        Bukkit.getServer().getPluginManager().callEvent(e);
        for (ExtraCursor c : e.getCursors()) {
            if (!c.getWorld().equalsIgnoreCase(player.getWorld().getName())) {
                continue;
            }

            int x = ((c.getX() - centerX) / scale) * 2;
            int z = ((c.getZ() - centerZ) / scale) * 2;

            if (Math.abs(x) > 127) {
                if (c.isShownOutside()) {
                    x = c.getX() > player.getLocation().getBlockX() ? 127 : -128;
                } else {
                    continue;
                }
            }

            if (Math.abs(z) > 127) {
                if (c.isShownOutside()) {
                    z = c.getZ() > player.getLocation().getBlockZ() ? 127 : -128;
                } else {
                    continue;
                }
            }
            cursors.addCursor(x, z, c.getDirection(), c.getType().getValue(), c.isVisible());
        }
    }

    private short getPrevY(int x, int z, String world, int scale) {
        final Map<Integer, Map<Integer, MapPixel>> cacheMap = worldCacheMap.get(world);

        if (cacheMap.containsKey(x + -scale) && cacheMap.get(x + -scale).containsKey(z + -scale)) {
            return cacheMap.get(x + -scale).get(z + -scale).getAverageY();
        }
        if (cacheMap.containsKey(x + scale) && cacheMap.get(x + scale).containsKey(z + scale)) {
            return cacheMap.get(x + scale).get(z + scale).getAverageY();
        }
        if (cacheMap.containsKey(x + -scale) && cacheMap.get(x + -scale).containsKey(z + scale)) {
            return cacheMap.get(x + -scale).get(z + scale).getAverageY();
        }
        if (cacheMap.containsKey(x + -scale) && cacheMap.get(x + -scale).containsKey(z + scale)) {
            return cacheMap.get(x + -scale).get(z + scale).getAverageY();
        }
        return 0;
    }

    private void handleBlockEvent(Block block) {
        addToQueue(new Coords(block.getX(), block.getZ(), block.getWorld().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockPlaceEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockFromToEvent e) {
        handleBlockEvent(e.getBlock());
        handleBlockEvent(e.getToBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockPhysicsEvent e) {
        switch (e.getChangedType()) {
            case LAVA:
            case WATER:
                handleBlockEvent(e.getBlock());
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockBreakEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockBurnEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockFadeEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockFormEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockGrowEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockSpreadEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(EntityBlockFormEvent e) {
        handleBlockEvent(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {

    }
}
