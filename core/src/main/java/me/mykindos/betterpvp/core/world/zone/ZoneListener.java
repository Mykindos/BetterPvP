package me.mykindos.betterpvp.core.world.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Drives per-player zone tracking off movement, teleport, respawn and join, delegating all logic to the
 * {@link ZoneManager}. Generic to every module: it carries no module-specific behaviour, only "recompute the zone for
 * where this player now is". Short-circuits entirely while no zones are registered, so servers not using zones pay
 * nothing.
 */
@BPvPListener
@Singleton
public class ZoneListener implements Listener {

    private final ZoneManager zoneManager;

    @Inject
    private ZoneListener(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onRespawn(PlayerRespawnEvent event) {
        update(event.getPlayer(), event.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock()) {
            update(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onTeleport(PlayerTeleportEvent event) {
        update(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onJoin(ClientJoinEvent event) {
        final Player player = event.getClient().getGamer().getPlayer();
        if (player != null) {
            update(player, player.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onQuit(PlayerQuitEvent event) {
        zoneManager.clear(event.getPlayer());
    }

    private void update(Player player, Location location) {
        if (!zoneManager.isActive()) {
            return;
        }
        zoneManager.updateZone(player, location);
    }
}
