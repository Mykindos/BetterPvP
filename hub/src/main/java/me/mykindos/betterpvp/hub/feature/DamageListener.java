package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

@BPvPListener
@Singleton
public class DamageListener implements Listener {

    private final HubWorld hubWorld;
    private final ZoneService zoneService;

    @Inject
    private DamageListener(HubWorld hubWorld, ZoneService zoneService) {
        this.hubWorld = hubWorld;
        this.zoneService = zoneService;
    }

    // Disable PvP outside FFA
    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player player && zoneService.getZone(player) != Zone.FFA) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Player player && zoneService.getZone(player) != Zone.FFA) {
            event.setCancelled(true);
        }
    }

    // this gets overwritten by FFA for ffa players
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(hubWorld.getSpawnpoint().getLocation());
    }


}
