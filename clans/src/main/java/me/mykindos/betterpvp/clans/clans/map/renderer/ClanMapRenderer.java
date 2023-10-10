package me.mykindos.betterpvp.clans.clans.map.renderer;


import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.ChunkData;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.core.components.clans.IClan;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

public class ClanMapRenderer extends MapRenderer {

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
        if (!mapHandler.enabled) return;
        currentInterval++;
        if(currentInterval < mapHandler.updateInterval){
            return;
        }
        currentInterval = 0;

        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) return;
        if (!player.getWorld().getName().equals("world")) return;

        MapSettings mapSettings = mapHandler.mapSettingsMap.get(player.getUniqueId());
        MapSettings.Scale s = mapSettings.getScale();

        final boolean hasMoved = mapHandler.hasMoved(player);
        if (!(hasMoved || mapSettings.isUpdate())) {
            return;
        }

        final MapCursorCollection cursors = mapCanvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        int scale = 1 << s.getValue();

        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();

        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                mapCanvas.setPixelColor(i, j, mapCanvas.getBasePixelColor(i, j));
            }
        }

        if (s == MapSettings.Scale.FAR) {
            centerX = 0;
            centerZ = 0;
        }


        for (ChunkData chunkData : mapHandler.clanMapData.get(player.getUniqueId())) {
            if (!chunkData.getWorld().equals(player.getWorld().getName())) continue;

            final IClan clan = chunkData.getClan();
            if (clan == null) continue;

            int bx = chunkData.getX() << 4; //Chunk's actual world coord;
            int bz = chunkData.getZ() << 4; //Chunk's actual world coord;

            int pX = (bx - centerX) / scale + 64; //Gets the pixel location;
            int pZ = (bz - centerZ) / scale + 64; //Gets the pixel location;

            final boolean admin = clan.isAdmin();

            byte chunkDataColor = chunkData.getColor().getPackedId(MapColor.Brightness.NORMAL);

            for (int cx = 0; cx < 16 / scale; cx++) {
                for (int cz = 0; cz < 16 / scale; cz++) {
                    if (pX + cx >= 0 && pX + cx < 128 && pZ + cz >= 0 && pZ + cz < 128) { //Checking if its in the maps bounds;
                        if (s.ordinal() <= MapView.Scale.CLOSE.ordinal() || admin) {
                            mapCanvas.setPixel(pX + cz, pZ + cz, chunkDataColor);
                        }

                        if (cx == 0) {
                            if (!chunkData.getBlockFaceSet().contains(BlockFace.WEST)) {
                                mapCanvas.setPixel(pX + cx, pZ + cz, chunkDataColor);
                            }
                        }
                        if (cx == (16 / scale) - 1) {
                            if (!chunkData.getBlockFaceSet().contains(BlockFace.EAST)) {
                                mapCanvas.setPixel(pX + cx, pZ + cz, chunkDataColor);
                            }
                        }
                        if (cz == 0) {
                            if (!chunkData.getBlockFaceSet().contains(BlockFace.NORTH)) {
                                mapCanvas.setPixel(pX + cx, pZ + cz, chunkDataColor);
                            }
                        }
                        if (cz == (16 / scale) - 1) {
                            if (!chunkData.getBlockFaceSet().contains(BlockFace.SOUTH)) {
                                mapCanvas.setPixel(pX + cx, pZ + cz, chunkDataColor);
                            }
                        }

                    }
                }
            }
        }
        mapHandler.updateLastMoved(player);
        mapSettings.setUpdate(false);
    }
}