package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class DamageListener implements Listener {

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            event.setCancelled(true); // TODO: Implement zoning for FFA arena
        }
    }

}
