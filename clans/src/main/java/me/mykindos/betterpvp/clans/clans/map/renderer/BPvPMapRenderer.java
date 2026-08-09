package me.mykindos.betterpvp.clans.clans.map.renderer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.cursor.MapCursorService;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.core.map.PointOfInterest;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

/**
 * The map's only renderer. Terrain, zone tint and clan territory arrive here already composed into a single byte array
 * by {@link me.mykindos.betterpvp.clans.clans.map.frame.MapFrameService}, so drawing is a copy; cursors are placed from
 * the per-tick snapshot in {@link MapCursorService}.
 * <p>
 * Being the sole renderer keeps the canvas to a single pass per viewer and leaves one owner for the cursor
 * collection, so no layer can clear another's cursors.
 */
@Singleton
public class BPvPMapRenderer extends MapRenderer {

    private static final int MAP_SIZE = MapSettings.MAP_SIZE;
    private static final int MAP_EDGE_POSITIVE = 127;
    private static final int MAP_EDGE_NEGATIVE = -128;
    private static final byte DEFAULT_DIRECTION = 8;

    private final MapHandler mapHandler;
    private final MapCursorService cursorService;

    @Inject
    public BPvPMapRenderer(MapHandler mapHandler, MapCursorService cursorService) {
        super(true);
        this.mapHandler = mapHandler;
        this.cursorService = cursorService;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!mapHandler.isEnabled()) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) {
            return;
        }

        final MapSettings settings = mapHandler.getOrCreateMapSettings(player);
        // Read the dirty flag before the frame: the builder writes the frame first and flips the flag last, so seeing
        // the flag set guarantees the array read after it is the new one.
        final boolean dirty = settings.isFrameDirty();
        final byte[] frame = settings.getFrame();
        final boolean hasNewFrame = dirty && frame != null;

        // A frame waiting to be shown always draws immediately — that is what makes a zoom land on the very next tick.
        // The interval only throttles the cursor refresh on a settled map.
        if (!hasNewFrame) {
            settings.setRenderInterval(settings.getRenderInterval() + 1);
            if (settings.getRenderInterval() < mapHandler.getUpdateInterval()) {
                return;
            }
        }
        settings.setRenderInterval(0);

        if (hasNewFrame) {
            blit(canvas, frame);
            settings.setFrameDirty(false);
        }

        drawCursors(canvas, player, settings);
    }

    @SuppressWarnings("deprecation") // The byte overload is the fast path; the Color one re-quantises per pixel.
    private void blit(MapCanvas canvas, byte[] frame) {
        for (int x = 0; x < MAP_SIZE; x++) {
            final int row = x * MAP_SIZE;
            for (int z = 0; z < MAP_SIZE; z++) {
                canvas.setPixel(x, z, frame[row + z]);
            }
        }
    }

    private void drawCursors(MapCanvas canvas, Player player, MapSettings settings) {
        final MapCursorCollection cursors = canvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        final int scale = settings.getScale().getValue();
        final int centerX = player.getLocation().getBlockX();
        final int centerZ = player.getLocation().getBlockZ();

        final MinimapExtraCursorEvent event =
                UtilServer.callEvent(new MinimapExtraCursorEvent(player, cursors, scale));
        for (ExtraCursor cursor : event.getCursors()) {
            addExtraCursor(cursors, player, cursor, scale, centerX, centerZ);
        }

        for (PointOfInterest pointOfInterest : cursorService.getPointsOfInterest()) {
            addPointOfInterest(cursors, player, pointOfInterest, scale, centerX, centerZ);
        }
    }

    private void addExtraCursor(MapCursorCollection cursors, Player player, ExtraCursor cursor,
                                int scale, int centerX, int centerZ) {
        if (cursor.getWorld() != null && !cursor.getWorld().equals(player.getWorld().getName())) {
            return;
        }

        int x = gridOffset(cursor.getX(), centerX, scale);
        int z = gridOffset(cursor.getZ(), centerZ, scale);

        if (Math.abs(x) > MAP_EDGE_POSITIVE) {
            if (!cursor.isShownOutside()) {
                return;
            }
            x = cursor.getX() > centerX ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
        }
        if (Math.abs(z) > MAP_EDGE_POSITIVE) {
            if (!cursor.isShownOutside()) {
                return;
            }
            z = cursor.getZ() > centerZ ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
        }

        cursors.addCursor(new MapCursor((byte) x, (byte) z, cursor.getDirection(),
                cursor.getType(), cursor.isVisible(), cursor.getCaption()));
    }

    /**
     * Cursor coordinates are half-pixels from the centre of the map. Both positions go through the sample grid first,
     * so a cursor sits on the same pixel the terrain drew for that block rather than drifting by one off the origin.
     */
    private int gridOffset(int position, int center, int scale) {
        return (Math.floorDiv(position, scale) - Math.floorDiv(center, scale)) * 2;
    }

    private void addPointOfInterest(MapCursorCollection cursors, Player player, PointOfInterest pointOfInterest,
                                    int scale, int centerX, int centerZ) {
        if (pointOfInterest.getLocation().getWorld() == null
                || !pointOfInterest.getLocation().getWorld().equals(player.getWorld())) {
            return;
        }

        int x = gridOffset(pointOfInterest.getLocation().getBlockX(), centerX, scale);
        int z = gridOffset(pointOfInterest.getLocation().getBlockZ(), centerZ, scale);

        if (Math.abs(x) > MAP_EDGE_POSITIVE) {
            x = pointOfInterest.getLocation().getBlockX() > centerX ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
        }
        if (Math.abs(z) > MAP_EDGE_POSITIVE) {
            z = pointOfInterest.getLocation().getBlockZ() > centerZ ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
        }

        cursors.addCursor(new MapCursor((byte) x, (byte) z, DEFAULT_DIRECTION,
                pointOfInterest.getType(), true, pointOfInterest.getName()));
    }
}
