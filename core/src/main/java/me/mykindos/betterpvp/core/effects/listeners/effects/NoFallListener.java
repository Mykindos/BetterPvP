package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

@Singleton
@BPvPListener
public class NoFallListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public NoFallListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                effectManager.getEffect(livingEntity, EffectTypes.NO_FALL).ifPresent(effect -> {
                    if (event.getDamage() <= effect.getAmplifier()) {
                        event.setCancelled(true);
                    } else {
                        event.setDamage(Math.max(0, event.getDamage() - effect.getAmplifier()));
                    }
                });
            }
        }
    }
}
