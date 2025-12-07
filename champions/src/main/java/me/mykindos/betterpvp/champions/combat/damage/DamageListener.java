package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

@Singleton
@BPvPListener
public class DamageListener implements Listener {

    @Inject
    @Config(path = "damage.multiplier.fall", defaultValue = "0.8")
    private double fallDamageMultiplier;

    @EventHandler (priority = EventPriority.LOWEST)
    public void onFallDamage(DamageEvent event) {
        if (event.isCancelled()) return;

        if(event.getBukkitCause() != EntityDamageEvent.DamageCause.FALL) return;
        if(event.getDamagee() instanceof Player) return;

        event.setDamage(event.getDamage() * fallDamageMultiplier);
    }

}
