package me.mykindos.betterpvp.clans.clans.map.renderer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.Coords;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.data.MapPixel;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.clans.clans.map.nms.UtilMapMaterial;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Getter
@Singleton
public class MinimapRenderer extends MapRenderer implements Listener {
    // Constants
    private static final int MAP_SIZE = 128;
    private static final int QUEUE_PROCESS_INTERVAL = 5;
    private static final int WHITE_COLOR = Color.WHITE.getRGB();
    private static final byte MAP_EDGE_POSITIVE = 127;
    private static final byte MAP_EDGE_NEGATIVE = -128;

    // Services
    private final MapHandler mapHandler;
    private final Clans clans;

    // Cache and queue
    protected Map<String, Map<Integer, Map<Integer, MapPixel>>> worldCacheMap = new HashMap<>();
    protected Queue<Coords> updateQueue = new LinkedList<>();

    // Configuration
    @Inject
    @Config(path = "clans.map.maxProcess", defaultValue = "64")
    private int maxProcess;

    @Inject
    @Config(path = "clans.map.maxMapDistance", defaultValue = "645")
    private int maxDistance;

    private int currentInterval = 1;

    @Inject
    public MinimapRenderer(MapHandler mapHandler, Clans clans) {
        super(true);
        this.mapHandler = mapHandler;
        this.clans = clans;
        Bukkit.getPluginManager().registerEvents(this, clans);
        UtilServer.runTaskTimer(clans, this::processUpdateQueue, QUEUE_PROCESS_INTERVAL, QUEUE_PROCESS_INTERVAL);
    }

    private void processUpdateQueue() {
        if (updateQueue.isEmpty()) return;

        for (int i = 0; i < maxProcess; i++) {
            final Coords coords = updateQueue.poll();
            if (coords == null) {
                return;
            }

            updateMapPixel(coords);
        }
    }

    private void updateMapPixel(Coords coords) {
        World world = Bukkit.getWorld(coords.getWorld());
        if (world == null) return;

        Map<String, Map<Integer, Map<Integer, MapPixel>>> worldCache = getWorldCacheMap();
        if (!worldCache.containsKey(coords.getWorld())) return;
        if (!worldCache.get(coords.getWorld()).containsKey(coords.getX())) return;
        if (!worldCache.get(coords.getWorld()).get(coords.getX()).containsKey(coords.getZ())) return;

        Block block = world.getBlockAt(coords.getX(), world.getHighestBlockYAt(coords.getX(), coords.getZ()), coords.getZ());
        if (!block.getChunk().isLoaded()) return;

        // Find first non-neutral block from top down
        while (block.getY() > 0 && UtilMapMaterial.getBlockColor(block) == UtilMapMaterial.getColorNeutral()) {
            block = world.getBlockAt(block.getX(), block.getY() - 1, block.getZ());
        }

        short blockHeight = (short) block.getY();
        var blockColor = UtilMapMaterial.getBlockColor(block);

        final MapPixel mapPixel = worldCache.get(block.getWorld().getName()).get(block.getX()).get(block.getZ());
        mapPixel.setAverageY(blockHeight);
        mapPixel.setColorId(blockColor.id);
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!mapHandler.isEnabled()) return;

        // Handle update interval
        currentInterval++;
        if (currentInterval < mapHandler.getUpdateInterval()) {
            return;
        }
        currentInterval = 0;

        // Only render for players holding maps
        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) return;

        // Handle non-main worlds
        if (!player.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) {
            renderBlankMap(canvas);
            return;
        }

        renderMinimapForPlayer(canvas, player);
    }

    private void renderBlankMap(MapCanvas canvas) {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                canvas.setPixelColor(x, z, Color.WHITE);
            }
        }
    }

    private void renderMinimapForPlayer(MapCanvas canvas, Player player) {
        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();

        // Get or create map settings for player
        int finalCenterX = centerX;
        int finalCenterZ = centerZ;
        final MapSettings mapSettings = mapHandler.getMapSettingsMap()
                .computeIfAbsent(player.getUniqueId(),
                        k -> new MapSettings(finalCenterX, finalCenterZ));

        int scale = mapSettings.getScale().getValue();

        // Adjust center for far-out scales
        if (mapSettings.getScale().ordinal() >= MapSettings.Scale.FAR.ordinal()) {
            centerX = 0;
            centerZ = 0;
        }

        // Ensure cache exists for player's world
        if (!worldCacheMap.containsKey(player.getWorld().getName())) {
            worldCacheMap.put(player.getWorld().getName(), new HashMap<>());
        }
        final Map<Integer, Map<Integer, MapPixel>> worldCache = worldCacheMap.get(player.getWorld().getName());

        boolean playerHasMoved = mapHandler.hasMoved(player);
        if (playerHasMoved || mapSettings.isUpdate()) {
            renderMapPixels(canvas, player, worldCache, centerX, centerZ, scale);
        }

        // Add cursors to map
        handleCursors(canvas, player, scale, centerX, centerZ);
    }

    private void renderMapPixels(MapCanvas canvas, Player player, Map<Integer, Map<Integer, MapPixel>> worldCache,
                                 int centerX, int centerZ, int scale) {
        int locX = centerX / scale - 64;
        int locZ = centerZ / scale - 64;

        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                int x = (locX + i) * scale;
                int z = (locZ + j) * scale;

                if (isOutOfBounds(x, z)) {
                    canvas.setPixelColor(i, j, Color.WHITE);
                    continue;
                }

                // Adjust coordinates for negative values
                if (locX + i < 0 && (locX + i) % scale != 0) x--;
                if (locZ + j < 0 && (locZ + j) % scale != 0) z--;

                renderPixel(canvas, player, worldCache, x, z, i, j, scale);
            }
        }
    }

    private boolean isOutOfBounds(int x, int z) {
        return x > maxDistance || x < -maxDistance || z > maxDistance || z < -maxDistance;
    }

    private void renderPixel(MapCanvas canvas, Player player, Map<Integer, Map<Integer, MapPixel>> worldCache,
                             int x, int z, int canvasX, int canvasZ, int scale) {
        var pixelX = worldCache.get(x);
        MapPixel mapPixel;

        if (pixelX != null && (mapPixel = pixelX.get(z)) != null) {
            short prevY = getPrevY(x, z, player.getWorld().getName(), scale);
            double heightDifference = calculateHeightDifference(mapPixel.getAverageY(), prevY, scale, canvasX, canvasZ);

            MapColor.Brightness brightness = determinePixelBrightness(heightDifference);
            MapColor materialColor = MapColor.byId(mapPixel.getColorId());

            canvas.setPixel(canvasX, canvasZ, materialColor.getPackedId(brightness));
        } else {
            // Cache surrounding pixels
            for (int k = -scale; k < scale; k++) {
                for (int l = -scale; l < scale; l++) {
                    cachePixel(worldCache, x + k, z + l, player);
                }
            }
        }
    }

    private double calculateHeightDifference(short currentY, short prevY, int scale, int i, int j) {
        return (currentY - prevY) * 4.0D / (scale + 4) + ((i + j & 1) - 0.5D) * 0.4D;
    }

    private MapColor.Brightness determinePixelBrightness(double heightDifference) {
        if (heightDifference > 0.6D) {
            return MapColor.Brightness.HIGH;
        } else if (heightDifference < -0.6D) {
            return MapColor.Brightness.LOW;
        }
        return MapColor.Brightness.NORMAL;
    }

    private void addToUpdateQueue(Coords coords) {
        if (!updateQueue.contains(coords)) {
            updateQueue.add(coords);
        }
    }

    private void cachePixel(Map<Integer, Map<Integer, MapPixel>> worldCache, int x, int z, Player player) {
        if (isOutOfBounds(x, z)) return;

        if (!worldCache.containsKey(x)) {
            worldCache.put(x, new HashMap<>());
        }
        Map<Integer, MapPixel> xMap = worldCache.get(x);

        if (!xMap.containsKey(z)) {
            Block block = player.getWorld().getHighestBlockAt(x, z, HeightMap.WORLD_SURFACE);
            if (!block.getChunk().isLoaded()) {
                return;
            }

            short blockHeight = (short) block.getY();
            var blockColor = UtilMapMaterial.getBlockColor(block).id;

            xMap.put(z, new MapPixel(blockColor, blockHeight));
        }
    }

    private void handleCursors(MapCanvas canvas, Player player, int scale, int centerX, int centerZ) {
        MapCursorCollection cursors = canvas.getCursors();

        // Clear existing cursors
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        // Add new cursors from event
        MinimapExtraCursorEvent cursorEvent = UtilServer.callEvent(new MinimapExtraCursorEvent(player, cursors, scale));
        for (ExtraCursor cursor : cursorEvent.getCursors()) {
            addCursorToMap(cursor, cursors, player, scale, centerX, centerZ);
        }
    }

    private void addCursorToMap(ExtraCursor cursor, MapCursorCollection cursors, Player player,
                                int scale, int centerX, int centerZ) {
        int x = ((cursor.getX() - centerX) / scale) * 2;
        int z = ((cursor.getZ() - centerZ) / scale) * 2;

        // Handle X coordinates beyond map edges
        if (Math.abs(x) > 127) {
            if (cursor.isShownOutside()) {
                x = cursor.getX() > player.getLocation().getBlockX() ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
            } else {
                return;
            }
        }

        // Handle Z coordinates beyond map edges
        if (Math.abs(z) > 127) {
            if (cursor.isShownOutside()) {
                z = cursor.getZ() > player.getLocation().getBlockZ() ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
            } else {
                return;
            }
        }

        MapCursor mapCursor = new MapCursor(
                (byte) x,
                (byte) z,
                cursor.getDirection(),
                cursor.getType(),
                cursor.isVisible(),
                cursor.getCaption()
        );
        cursors.addCursor(mapCursor);
    }

    private short getPrevY(int x, int z, String world, int scale) {
        final Map<Integer, Map<Integer, MapPixel>> worldCache = worldCacheMap.get(world);

        // Check top-left
        Map<Integer, MapPixel> xMinusScale = worldCache.get(x - scale);
        if (xMinusScale != null) {
            MapPixel zMinusScale = xMinusScale.get(z - scale);
            if (zMinusScale != null) {
                return zMinusScale.getAverageY();
            }

            // Check bottom-left
            MapPixel zPlusScale = xMinusScale.get(z + scale);
            if (zPlusScale != null) {
                return zPlusScale.getAverageY();
            }
        }

        // Check bottom-right
        Map<Integer, MapPixel> xPlusScale = worldCache.get(x + scale);
        if (xPlusScale != null) {
            MapPixel zPlusScale = xPlusScale.get(z + scale);
            if (zPlusScale != null) {
                return zPlusScale.getAverageY();
            }
        }

        return 0;
    }

    private void handleBlockEvent(Block block) {
        if (block.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) {
            addToUpdateQueue(new Coords(block.getX(), block.getZ(), block.getWorld().getName()));
        }
    }

    // Consolidated block event handlers with consistent priority and ignoreCancelled settings
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        handleBlockEvent(event.getBlock());
        handleBlockEvent(event.getToBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        switch (event.getChangedType()) {
            case LAVA, WATER -> handleBlockEvent(event.getBlock());
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        handleBlockEvent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        // Method intentionally empty, placeholder for future implementation
    }
}