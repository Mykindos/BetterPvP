package me.mykindos.betterpvp.clans.clans.map.renderer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.core.map.PointOfInterest;
import me.mykindos.betterpvp.core.map.events.MapPointOfInterestEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class PointOfInterestRenderer extends MapRenderer {

    private static final int MAP_EDGE_POSITIVE = 127;
    private static final int MAP_EDGE_NEGATIVE = -128;
    private static final byte DEFAULT_DIRECTION = 8;
    private static final long CACHE_REFRESH_INTERVAL = 20L * 5;

    private final MapHandler mapHandler;
    private final List<PointOfInterest> cachedPointsOfInterest = new ArrayList<>();

    private int currentInterval = 1;

    @Inject
    public PointOfInterestRenderer(MapHandler mapHandler, Clans clans) {
        super(true);
        this.mapHandler = mapHandler;
        refreshPointsOfInterest();
        UtilServer.runTaskTimer(clans, this::refreshPointsOfInterest, CACHE_REFRESH_INTERVAL, CACHE_REFRESH_INTERVAL);
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!mapHandler.isEnabled()) return;

        MapCursorCollection cursors = canvas.getCursors();
        while (cursors.size() > 0) {
            cursors.removeCursor(cursors.getCursor(0));
        }

        currentInterval++;
        if (currentInterval < mapHandler.getUpdateInterval()) {
            return;
        }
        currentInterval = 0;

        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) return;
        if (!player.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) return;

        final int playerCenterX = player.getLocation().getBlockX();
        final int playerCenterZ = player.getLocation().getBlockZ();
        final MapSettings mapSettings = mapHandler.getMapSettingsMap()
                .computeIfAbsent(player.getUniqueId(), ignored -> new MapSettings(playerCenterX, playerCenterZ));
        final int scale = mapSettings.getScale().getValue();

        int centerX = playerCenterX;
        int centerZ = playerCenterZ;

        if (mapSettings.getScale().ordinal() >= MapSettings.Scale.FARTHEST.ordinal()) {
            centerX = 0;
            centerZ = 0;
        }

        synchronized (cachedPointsOfInterest) {
            for (PointOfInterest pointOfInterest : cachedPointsOfInterest) {
                addPointOfInterest(cursors, player, pointOfInterest, scale, centerX, centerZ);
            }
        }
    }

    private void refreshPointsOfInterest() {
        MapPointOfInterestEvent event = UtilServer.callEvent(new MapPointOfInterestEvent());
        synchronized (cachedPointsOfInterest) {
            cachedPointsOfInterest.clear();
            cachedPointsOfInterest.addAll(event.getPointsOfInterest());
        }
    }

    private void addPointOfInterest(MapCursorCollection cursors, Player player, PointOfInterest pointOfInterest,
                                    int scale, int centerX, int centerZ) {
        if (pointOfInterest.getLocation().getWorld() == null) {
            return;
        }

        if (!pointOfInterest.getLocation().getWorld().equals(player.getWorld())) {
            return;
        }

        int x = ((pointOfInterest.getLocation().getBlockX() - centerX) / scale) * 2;
        int z = ((pointOfInterest.getLocation().getBlockZ() - centerZ) / scale) * 2;

        if (Math.abs(x) > 127) {
            x = pointOfInterest.getLocation().getBlockX() > player.getLocation().getBlockX() ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
        }

        if (Math.abs(z) > 127) {
            z = pointOfInterest.getLocation().getBlockZ() > player.getLocation().getBlockZ() ? MAP_EDGE_POSITIVE : MAP_EDGE_NEGATIVE;
        }

        cursors.addCursor(new MapCursor(
                (byte) x,
                (byte) z,
                DEFAULT_DIRECTION,
                pointOfInterest.getType(),
                true,
                pointOfInterest.getName()
        ));
    }
}