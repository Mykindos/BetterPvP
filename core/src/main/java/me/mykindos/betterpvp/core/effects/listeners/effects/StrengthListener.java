package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
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
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.getDamager() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectTypes.STRENGTH);
            effectOptional.ifPresent(effect -> event.setDamage(event.getDamage() + (1.5 * effect.getAmplifier())));
        }
    }
}
