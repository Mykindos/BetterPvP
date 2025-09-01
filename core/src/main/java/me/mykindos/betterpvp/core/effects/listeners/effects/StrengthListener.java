package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
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
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;

@BPvPListener
@Singleton
public class StrengthListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public StrengthListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onStrengthDamage(DamageEvent event) {
        if (event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            return;
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        Optional<Effect> effectOptional = effectManager.getEffect(player, EffectTypes.STRENGTH);
        if (effectOptional.isEmpty()) {
            return;
        }

        final Effect effect = effectOptional.get();
        final double increment = event.getDamage() + (1.5 * effect.getAmplifier());
        event.addModifier(new GenericModifier("Strength", 1.0, increment).withType(ModifierType.EFFECT));
    }
}
