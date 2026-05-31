package me.mykindos.betterpvp.hub.feature.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Keeps players inside the hub world's bounding box, teleporting escapees back to spawn. This is world-bounds
 * enforcement, not zone logic, so it lives in the hub rather than the generic core zone listener.
 */
@BPvPListener
@Singleton
public class HubBoundsListener implements Listener {

    private final HubWorld hubWorld;
    private final ClientManager clientManager;

    @Inject
    private HubBoundsListener(HubWorld hubWorld, ClientManager clientManager) {
        this.hubWorld = hubWorld;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || clientManager.search().online(player).isAdministrating()) {
            return;
        }

        final Location to = event.getTo();
        if (to.getWorld() == hubWorld.getWorld() && !hubWorld.isInsideBoundingBox(to)) {
            event.setTo(hubWorld.getSpawnpoint().getLocation());
        }
    }
}
