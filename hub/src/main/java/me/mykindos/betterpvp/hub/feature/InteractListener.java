package me.mykindos.betterpvp.hub.feature;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@BPvPListener
@Singleton
public class InteractListener implements Listener {

    private final ZoneService zoneService;

    @Inject
    public InteractListener(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @EventHandler
    public void onExperience(PlayerPickupExperienceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (GameMode.CREATIVE.equals(event.getPlayer().getGameMode())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (GameMode.CREATIVE.equals(event.getPlayer().getGameMode())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (GameMode.CREATIVE.equals(event.getPlayer().getGameMode())) {
            return;
        }

        if (zoneService.getZone(event.getPlayer()) == Zone.COMMON) {
            event.setCancelled(true);
        }
    }

}
