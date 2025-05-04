package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.types.negative.PinEffect;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@Singleton
public class PinListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public PinListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(CustomEntityVelocityEvent event) {
        final Entity rawEntity = event.getEntity();
        if (!(rawEntity instanceof LivingEntity entity)) return;

        Optional<Effect> effectOpt = effectManager.getEffect(entity, EffectTypes.PIN);
        if (effectOpt.isPresent() && event.getVelocityType().equals(VelocityType.KNOCKBACK)) {
            event.setCancelled(true); // cancel all knockback
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        final LivingEntity entity = event.getPlayer();
        Optional<Effect> effectOpt = effectManager.getEffect(entity, EffectTypes.PIN);
        if (effectOpt.isEmpty()) {
            return;
        }

        final Effect effect = effectOpt.get();
        final PinEffect type = (PinEffect) effect.getEffectType();
        type.getInteractionsLeft().compute(entity.getUniqueId(), (uuid, remaining) -> {
            final int value = remaining == null ? 0 : remaining - 1;
            if (value <= 0) {
                this.effectManager.removeEffect(entity, EffectTypes.PIN, effect.getName(), true);
                return null;
            }
            return value;
        });
    }
}