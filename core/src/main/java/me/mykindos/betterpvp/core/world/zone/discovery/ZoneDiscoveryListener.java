package me.mykindos.betterpvp.core.world.zone.discovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.events.ClientUnloadEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Wires the zone-discovery system into the event bus: detects first-visit discoveries on zone entry, preloads a
 * client's discoveries when they load, and clears the cache when they unload.
 */
@BPvPListener
@Singleton
public class ZoneDiscoveryListener implements Listener {

    private final ZoneDiscoveryService discoveryService;

    @Inject
    public ZoneDiscoveryListener(ZoneDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @EventHandler
    public void onEnterZone(PlayerEnterZoneEvent event) {
        discoveryService.discover(event.getPlayer(), event.getZone());
    }

    @EventHandler
    public void onClientLoad(AsyncClientLoadEvent event) {
        discoveryService.loadForClient(event.getClient());
    }

    @EventHandler
    public void onClientUnload(ClientUnloadEvent event) {
        discoveryService.clear(event.getClient().getUniqueId());
    }
}
