package me.mykindos.betterpvp.hub.feature.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;

@BPvPListener
@Singleton
public class ZoneListener implements Listener {

    private final HubWorld hubWorld;
    private final Map<Zone, CuboidRegion> regions = new HashMap<>();
    private final ClientManager clientManager;
    private final ZoneService zoneService;

    @Inject
    private ZoneListener(HubWorld hubWorld, ClientManager clientManager, ZoneService zoneService) {
        this.hubWorld = hubWorld;
        this.clientManager = clientManager;
        this.zoneService = zoneService;

        for (Zone zone : Zone.values()) {
            for (Region region : this.hubWorld.getRegions()) {
                if (!(region instanceof CuboidRegion cuboidRegion)) {
                    continue; // Not a cuboid
                }

                if (region.getName().equalsIgnoreCase(zone.name())) {
                    this.regions.put(zone, cuboidRegion);
                    break;
                }
            }

            // None were found, throw an error
            throw new IllegalStateException("No CuboidRegion found for zone: " + zone.name());
        }

    }

    // Check for when they leave and exit a zone
    @EventHandler
    void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return; // Haven't moved
        }

        // Find the zone they are moving into
        final Location location = event.getTo();
        Zone movedTo = Zone.NONE;
        if (location.getWorld() == hubWorld.getWorld()) {
            movedTo = Zone.COMMON;
            for (Zone zone : regions.keySet()) {
                CuboidRegion region = regions.get(zone);

                final BoundingBox box = new BoundingBox(
                        region.getMin().x(), region.getMin().y(), region.getMin().z(),
                        region.getMax().x(), region.getMax().y(), region.getMax().z()
                );

                // They are in this zone
                if (box.contains(location.toVector())) {
                    movedTo = zone;
                    break;
                }
            }
        }

        // Check if they switched zones
        final Zone previous = zoneService.getZone(event.getPlayer());
        if (previous != movedTo) {
            zoneService.enterZone(event.getPlayer(), movedTo);
        }
    }

    // Prevent people from exiting bounds
    @EventHandler
    void onExitHub(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)
                || event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)
                || clientManager.search().online(event.getPlayer()).isAdministrating()) {
            return; // they can do whatever
        }

        final Location to = event.getTo();
        if (to.getWorld() != hubWorld.getWorld()) {
            return; // Means an admin teleported them, we don't care
        }

        // At this point, they're in the hub world and they don't have perms to leave
        if (!hubWorld.isInsideBoundingBox(to)) {
            // Teleport them to spawn
            event.getPlayer().teleport(hubWorld.getSpawnpoint().getLocation());
        }
    }

}
