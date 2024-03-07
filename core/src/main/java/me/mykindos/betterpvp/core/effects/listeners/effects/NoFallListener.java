package me.mykindos.betterpvp.core.effects.listeners.effects;

import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

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
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (event.getEntity() instanceof Player) {
                if (effectManager.hasEffect((Player) event.getEntity(), EffectTypes.NO_FALL)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
