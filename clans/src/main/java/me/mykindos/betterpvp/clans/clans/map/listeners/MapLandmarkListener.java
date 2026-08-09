package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.clans.clans.map.data.ExtraCursor;
import me.mykindos.betterpvp.clans.clans.map.events.MinimapExtraCursorEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.map.MapCursor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adds a cursor for a player's last death location, contributed through {@link MinimapExtraCursorEvent} so the unified
 * cursor pass renders it. The marker is transient navigation help: it clears once it has been up for
 * {@link #DEATH_MARKER_MILLIS}, or once the player gets within {@link #DEATH_EXPIRE_CHUNKS} chunks of it.
 */
@BPvPListener
@Singleton
public class MapLandmarkListener implements Listener {

    private static final long DEATH_MARKER_MILLIS = 5 * 60 * 1000L;
    private static final int DEATH_EXPIRE_CHUNKS = 5;

    private final Map<UUID, DeathMark> deaths = new ConcurrentHashMap<>();

    @Inject
    @Config(path = "clans.map.landmarks.death", defaultValue = "true")
    private boolean showDeath;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        deaths.put(event.getEntity().getUniqueId(),
                new DeathMark(event.getEntity().getLocation(), System.currentTimeMillis() + DEATH_MARKER_MILLIS));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        deaths.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCursor(MinimapExtraCursorEvent event) {
        if (!showDeath) {
            return;
        }

        final Player player = event.getPlayer();
        final DeathMark mark = deaths.get(player.getUniqueId());
        if (mark == null) {
            return;
        }

        final Location death = mark.getLocation();
        if (System.currentTimeMillis() > mark.getExpiresAt()
                || death.getWorld() == null || !death.getWorld().equals(player.getWorld())
                || withinChunks(player.getLocation(), death, DEATH_EXPIRE_CHUNKS)) {
            deaths.remove(player.getUniqueId());
            return;
        }

        event.getCursors().add(new ExtraCursor(death.getBlockX(), death.getBlockZ(), true,
                MapCursor.Type.RED_X, (byte) 8, death.getWorld().getName(), true, "Death"));
    }

    private boolean withinChunks(Location a, Location b, int chunks) {
        final int dcx = Math.abs((a.getBlockX() >> 4) - (b.getBlockX() >> 4));
        final int dcz = Math.abs((a.getBlockZ() >> 4) - (b.getBlockZ() >> 4));
        return Math.max(dcx, dcz) <= chunks;
    }

    @Value
    private static class DeathMark {
        Location location;
        long expiresAt;
    }
}
