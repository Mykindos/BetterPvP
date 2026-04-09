package me.mykindos.betterpvp.hub.feature.ffa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;

@BPvPListener
@Singleton
public class FFADamageListener implements Listener {

    private final FFARegionService ffaRegionService;

    @Inject
    public FFADamageListener(FFARegionService ffaRegionService) {
        this.ffaRegionService = ffaRegionService;
    }

    @EventHandler
    void onVelocity(CustomEntityVelocityEvent event) {
        if (!ffaRegionService.contains(event.getSource().getLocation())) {
            return; // Damager is not in FFA
        }

        if (!ffaRegionService.contains(event.getEntity().getLocation())) {
            event.setCancelled(true); // Target is not in FFA, cancel velocity change
        }
    }

    @EventHandler
    void onFire(EntityCombustByEntityEvent event) {
        if (!ffaRegionService.contains(event.getCombuster().getLocation())) {
            return; // Damager is not in FFA
        }

        if (!ffaRegionService.contains(event.getEntity().getLocation())) {
            event.setCancelled(true); // Target is not in FFA, cancel combustion
        }
    }

    @EventHandler
    void onDamage(DamageEvent event) {
        if (event.getDamager() == null || !ffaRegionService.contains(event.getDamager().getLocation())) {
            return; // Damager is not in FFA
        }

        if (!ffaRegionService.contains(event.getDamagee().getLocation())) {
            event.setCancelled(true); // Target is not in FFA, cancel damage
        }
    }

}
