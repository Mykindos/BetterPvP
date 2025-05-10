package me.mykindos.betterpvp.clans.clans.map.renderer;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ClanMapRenderer extends MapRenderer {

    private static final int MAP_SIZE = 128;
    private static final int MAP_CENTER = 64;
    private static final int CHUNK_WIDTH = 16;

    private final MapHandler mapHandler;
    private int currentInterval;

    @Inject
    public ClanMapRenderer(MapHandler mapHandler) {
        super(true);
        this.mapHandler = mapHandler;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
        if (!isRenderingAllowed(player)) return;

        MapSettings mapSettings = getOrCreateMapSettings(player);
        MapSettings.Scale scale = mapSettings.getScale();

        if (!shouldUpdateMap(player, mapSettings)) return;

        clearCursors(mapCanvas);
        resetCanvasPixels(mapCanvas);

        int centerX = getCenterX(player, scale);
        int centerZ = getCenterZ(player, scale);
        int scaleValue = scale.getValue();

        renderChunks(player, mapCanvas, centerX, centerZ, scaleValue);

        mapHandler.updateLastMoved(player);
        mapSettings.setUpdate(false);
    }

    private boolean isRenderingAllowed(Player player) {
        if (!mapHandler.isEnabled()) return false;

        // Handle update interval
        currentInterval++;
        if (currentInterval < mapHandler.getUpdateInterval()) {
            return false;
        }
        currentInterval = 0;

        // Check if player has map in hand
        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) return false;

        // Check if player is in main world
        return player.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME);
    }

    private MapSettings getOrCreateMapSettings(Player player) {
        return mapHandler.getMapSettingsMap().computeIfAbsent(
                player.getUniqueId(),
                k -> new MapSettings(player.getLocation().getBlockX(), player.getLocation().getBlockZ())
        );
    }

    private boolean shouldUpdateMap(Player player, MapSettings mapSettings) {
        return mapHandler.hasMoved(player) || mapSettings.isUpdate();
    }

    private void clearCursors(MapCanvas mapCanvas) {
        final MapCursorCollection cursors = mapCanvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }
    }

    private void resetCanvasPixels(MapCanvas mapCanvas) {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                mapCanvas.setPixelColor(i, j, mapCanvas.getBasePixelColor(i, j));
            }
        }
    }

    private int getCenterX(Player player, MapSettings.Scale scale) {
        if (scale.ordinal() >= MapSettings.Scale.FAR.ordinal()) {
            return 0;
        }
        return player.getLocation().getBlockX();
    }

    private int getCenterZ(Player player, MapSettings.Scale scale) {
        if (scale.ordinal() >= MapSettings.Scale.FAR.ordinal()) {
            return 0;
        }
        return player.getLocation().getBlockZ();
    }

    private void renderChunks(Player player, MapCanvas mapCanvas, int centerX, int centerZ, int scale) {
        Set<ChunkData> playerChunks = mapHandler.getClanMapData().get(player.getUniqueId());
        if (playerChunks == null) return;

        for (ChunkData chunkData : playerChunks) {
            if (!isValidChunkData(chunkData, player)) continue;

            renderChunk(mapCanvas, chunkData, centerX, centerZ, scale);
        }
    }

    private boolean isValidChunkData(ChunkData chunkData, Player player) {
        if (!chunkData.getWorld().equals(player.getWorld().getName())) return false;

        final IClan clan = chunkData.getClan();
        return clan != null;
    }

    private void renderChunk(MapCanvas mapCanvas, ChunkData chunkData, int centerX, int centerZ, int scale) {
        IClan clan = chunkData.getClan();
        boolean isAdminClan = clan.isAdmin();

        // Convert chunk coordinates to world coordinates
        int worldX = chunkData.getX() << 4; // Chunk's actual world coord
        int worldZ = chunkData.getZ() << 4; // Chunk's actual world coord

        // Convert world coordinates to pixel coordinates on map
        int pixelX = (worldX - centerX) / scale + MAP_CENTER;
        int pixelZ = (worldZ - centerZ) / scale + MAP_CENTER;

        byte chunkColor = chunkData.getColor().getPackedId(MapColor.Brightness.NORMAL);
        int chunkSize = Math.max(1, (int) Math.ceil(CHUNK_WIDTH / (double) scale));

        renderChunkPixels(mapCanvas, chunkData, pixelX, pixelZ, scale, chunkSize, chunkColor, isAdminClan);
    }

    private void renderChunkPixels(MapCanvas mapCanvas, ChunkData chunkData, int pixelX, int pixelZ,
                                   int scale, int chunkSize, byte chunkColor, boolean isAdminClan) {
        MapSettings.Scale s = getScaleFromValue(scale);

        for (int cx = 0; cx < chunkSize; cx++) {
            for (int cz = 0; cz < chunkSize; cz++) {
                if (!isPixelInMapBounds(pixelX + cx, pixelZ + cz)) continue;

                int x = pixelX + cx;
                int z = pixelZ + cz;

                // Render admin or close-scale diagonal lines
                if (s.ordinal() <= MapView.Scale.CLOSE.ordinal() || isAdminClan) {
                    int diaX = pixelX + cz;
                    int diaZ = pixelZ + cz;
                    if (isPixelInMapBounds(diaX, diaZ)) {
                        mapCanvas.setPixel(diaX, diaZ, chunkColor);
                    }
                }

                // Render far scale non-admin pixels
                if (!isAdminClan && s.ordinal() >= MapView.Scale.FAR.ordinal()) {
                    mapCanvas.setPixel(x, z, chunkColor);
                }

                // Render chunk borders
                renderChunkBorders(mapCanvas, chunkData, cx, cz, x, z, scale, chunkColor);
            }
        }
    }

    private boolean isPixelInMapBounds(int x, int z) {
        return x >= 0 && x < MAP_SIZE && z >= 0 && z < MAP_SIZE;
    }

    private MapSettings.Scale getScaleFromValue(int scaleValue) {
        for (MapSettings.Scale scale : MapSettings.Scale.values()) {
            if (scale.getValue() == scaleValue) {
                return scale;
            }
        }
        return MapSettings.Scale.NORMAL;
    }

    private void renderChunkBorders(MapCanvas mapCanvas, ChunkData chunkData, int cx, int cz,
                                    int x, int z, int scale, byte chunkColor) {
        Set<BlockFace> blockFaces = chunkData.getBlockFaceSet();

        // West border
        if (cx == 0 && !blockFaces.contains(BlockFace.WEST)) {
            mapCanvas.setPixel(x, z, chunkColor);
        }

        // East border
        if (cx == getChunkEdgeIndex(scale) && !blockFaces.contains(BlockFace.EAST)) {
            mapCanvas.setPixel(x, z, chunkColor);
        }

        // North border
        if (cz == 0 && !blockFaces.contains(BlockFace.NORTH)) {
            mapCanvas.setPixel(x, z, chunkColor);
        }

        // South border
        if (cz == getChunkEdgeIndex(scale) && !blockFaces.contains(BlockFace.SOUTH)) {
            mapCanvas.setPixel(x, z, chunkColor);
        }
    }

    private int getChunkEdgeIndex(int scale) {
        return (CHUNK_WIDTH / scale) - 1;
    }
}