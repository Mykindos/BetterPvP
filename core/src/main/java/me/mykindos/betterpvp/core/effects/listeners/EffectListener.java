package me.mykindos.betterpvp.core.effects.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;
import java.util.UUID;

@BPvPListener
public class EffectListener implements Listener {

    private final EffectManager effectManager;


    @Inject
    public EffectListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        effectManager.removeAllEffects(event.getEntity());
    }

    @UpdateEvent (priority = 999)
    public void onUpdate() {
        effectManager.getObjects().forEach((key, value) -> {
            value.removeIf(effect -> {
                Entity entity = Bukkit.getEntity(UUID.fromString(effect.getUuid()));
                if (entity instanceof LivingEntity livingEntity) {
                    if (effect.hasExpired() && !effect.isPermanent()) {

                        UtilServer.callEvent(new EffectExpireEvent(livingEntity, effect));
                        effect.getEffectType().onExpire(livingEntity, effect);

                        return true;

                    } else {
                        if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                            vanillaEffectType.checkActive(livingEntity, effect);
                        }
                    }

                    effect.getEffectType().onTick(livingEntity, effect);
                } else {
                    return true;
                }

                return false;
            });
        });

        effectManager.getObjects().entrySet().removeIf(entry -> entry.getValue().isEmpty());

    }

    @EventHandler
    public void onEventClear(EffectClearEvent event) {
        effectManager.removeNegativeEffects(event.getPlayer());
    }

}
