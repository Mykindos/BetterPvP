package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void poisonDamageMultiplier(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.POISON) return;
        Optional<Effect> effectOptional = effectManager.getEffect(event.getDamagee(), EffectTypes.POISON);
        effectOptional.ifPresent(effect -> {
            if (event.getDamage() * effect.getAmplifier() < event.getDamagee().getHealth()) {
                event.setDamage(Math.min(event.getDamagee().getHealth(), event.getDamage() * effect.getAmplifier()));
            }
        });
    }

}
