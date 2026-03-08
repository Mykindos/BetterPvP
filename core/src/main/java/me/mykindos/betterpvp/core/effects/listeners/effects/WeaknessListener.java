package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.combat.modifiers.impl.GenericModifier;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@Singleton
public class WeaknessListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public WeaknessListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void resistanceReduction(DamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        Optional<Effect> effectOptional = effectManager.getEffect(player, EffectTypes.WEAKNESS);
        if (effectOptional.isEmpty()) {
            return;
        }

        final Effect effect = effectOptional.get();
        event.addModifier(new GenericModifier("Weakness",
                DamageOperator.MULTIPLIER,
                (1.0 - (double) (effect.getAmplifier() * 20) / 100)
        ).withType(ModifierType.EFFECT));
    }
}
