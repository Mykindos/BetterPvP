package me.mykindos.betterpvp.hub.feature.ffa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.blocks.RestoreBlockPlaceEvent;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class FFARestoreBlockListener implements Listener {

    private final HubWorld hubWorld;
    private final FFARegionService ffaRegionService;

    @Inject
    public FFARestoreBlockListener(HubWorld hubWorld, FFARegionService ffaRegionService) {
        this.hubWorld = hubWorld;
        this.ffaRegionService = ffaRegionService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRestoreBlockPlace(RestoreBlockPlaceEvent event) {
        if (event.getBlock().getWorld() != hubWorld.getWorld()) {
            return;
        }

        if (ffaRegionService.contains(event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);
    }
}
