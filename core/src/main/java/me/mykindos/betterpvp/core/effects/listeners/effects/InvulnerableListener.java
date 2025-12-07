package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class InvulnerableListener implements Listener {


    private final EffectManager effectManager;

    @Inject
    public InvulnerableListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void entDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            if (effectManager.hasEffect(damagee, EffectTypes.INVULNERABLE)) {
                event.setCancelled(true);
            }
        }
    }


}
