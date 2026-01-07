package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.feature.zone.Zone;
import me.mykindos.betterpvp.hub.feature.zone.ZoneService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class DamageListener implements Listener {

    private final ZoneService zoneService;

    @Inject
    private DamageListener(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    // Disable PvP outside FFA
    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player player && zoneService.getZone(player) != Zone.FFA) {
            event.setCancelled(true);
        }
    }

}
