package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

@Singleton
@BPvPListener
public class EntangledListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    private EntangledListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(CustomEntityVelocityEvent event) {
        final Entity rawEntity = event.getEntity();
        if (!(rawEntity instanceof LivingEntity entity)) return;

        Optional<Effect> effectOpt = effectManager.getEffect(entity, EffectTypes.ENTANGLED);
        if (effectOpt.isPresent() && !event.getVelocityType().equals(VelocityType.KNOCKBACK_CUSTOM)) {
            event.setCancelled(true); // cancel all knockback except for applied by others
        }
    }

}
