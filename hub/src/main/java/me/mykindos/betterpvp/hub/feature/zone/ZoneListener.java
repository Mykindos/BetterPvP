package me.mykindos.betterpvp.hub.feature.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.feature.ffa.FFARegionService;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@BPvPListener
@Singleton
public class ZoneListener implements Listener {

    private final HubWorld hubWorld;
    private final ClientManager clientManager;
    private final ZoneService zoneService;
    private final FFARegionService ffaRegionService;

    @Inject
    private ZoneListener(HubWorld hubWorld, ClientManager clientManager, ZoneService zoneService, FFARegionService ffaRegionService) {
        this.hubWorld = hubWorld;
        this.clientManager = clientManager;
        this.zoneService = zoneService;
        this.ffaRegionService = ffaRegionService;
    }

    @EventHandler
    void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                && event.getPlayer().getGameMode() != GameMode.SPECTATOR
                && !clientManager.search().online(event.getPlayer()).isAdministrating()) {
            final Location to = event.getTo();
            if (to.getWorld() == hubWorld.getWorld() && !hubWorld.isInsideBoundingBox(to)) {
                event.setTo(hubWorld.getSpawnpoint().getLocation());
            }
        }

        updateZone(event.getPlayer(), event.getTo());
    }

    @EventHandler(ignoreCancelled = true)
    void onTeleport(PlayerTeleportEvent event) {
        updateZone(event.getPlayer(), event.getTo());
    }

    @EventHandler
    void onJoin(ClientJoinEvent event) {
        final Player player = event.getClient().getGamer().getPlayer();
        if (player != null) {
            updateZone(player, player.getLocation());
        }
    }

    private void updateZone(Player player, Location location) {
        final Zone movedTo = resolveZone(location);
        if (zoneService.getZone(player) != movedTo) {
            zoneService.enterZone(player, movedTo);
        }
    }

    private Zone resolveZone(Location location) {
        if (location == null || location.getWorld() != hubWorld.getWorld()) {
            return Zone.NONE;
        }

        if (ffaRegionService.contains(location)) {
            return Zone.FFA;
        }

        return Zone.COMMON;
    }
}
