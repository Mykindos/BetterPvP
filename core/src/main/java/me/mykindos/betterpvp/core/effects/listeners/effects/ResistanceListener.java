package me.mykindos.betterpvp.core.effects.listeners.effects;

import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@BPvPListener
@Singleton
public class ResistanceListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public ResistanceListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void resistanceReduction(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectTypes.RESISTANCE);
            effectOptional.ifPresent(effect -> event.setDamage(event.getDamage() * (1.0 - (effect.getAmplifier() * 20) * 0.01)));
        }
    }
}
