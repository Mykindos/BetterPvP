package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@BPvPListener
@Singleton
public class ProtectionListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public ProtectionListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void entityDamageEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (effectManager.hasEffect(player, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player damagee && event.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damagee, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "This is a new player and has protection!");
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(damager, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "You cannot damage other players while you have protection!");
                UtilMessage.message(damager, "Protected", "Type '/protection' to disable this permanently.");
                event.setCancelled(true);
            }
        }
    }

}
