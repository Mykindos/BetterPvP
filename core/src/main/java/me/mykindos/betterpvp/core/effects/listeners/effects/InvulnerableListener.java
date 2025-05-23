package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class InvulnerableListener implements Listener {


    private final EffectManager effectManager;

    @Inject
    public InvulnerableListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }


    @EventHandler
    public void entDamage(PreCustomDamageEvent event) {
        if (event.getCustomDamageEvent().getDamagee() instanceof Player damagee) {
            if (effectManager.hasEffect(damagee, EffectTypes.INVULNERABLE)) {
                event.setCancelled(true);
            }

        }
    }


}
