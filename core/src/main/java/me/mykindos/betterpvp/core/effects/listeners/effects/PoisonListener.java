package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;

@Singleton
@BPvPListener
public class PoisonListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public PoisonListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void poisonDamageMultiplier(DamageEvent event) {
        if (event.getBukkitCause() != EntityDamageEvent.DamageCause.POISON) return;
        if (!event.isDamageeLiving()) return;
        final LivingEntity damagee = event.getLivingDamagee();
        Optional<Effect> effectOptional = effectManager.getEffect(damagee, EffectTypes.POISON);
        effectOptional.ifPresent(effect -> {
            // the damagee is below 2 health, poison does not damage below this value
            if (damagee.getHealth() <= 2) {
                event.setCancelled(true);
                return;
            }

            event.setDamage(Math.min(damagee.getHealth(), event.getDamage() * effect.getAmplifier()));
            if (damagee.getHealth() - event.getDamage() < 2) {
                //set damage to make the final damage leave the player at 2 health
                event.setDamage(damagee.getHealth() - 2);
            }
        });
    }

}
