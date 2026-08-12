package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.effects.types.negative.PoisonEffect;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class PoisonListener implements Listener {

    private final EffectManager effectManager;
    private final Map<LivingEntity, PoisonData> poisonTicks = new WeakHashMap<>();

    @Inject
    public PoisonListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onEffectReceive(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() instanceof PoisonEffect) {
            LivingEntity player = event.getTarget();

            final Effect effect = event.getEffect();
            final int level = effect.getAmplifier();
            final PoisonData data = new PoisonData(level, System.currentTimeMillis() + (long) (EffectTypes.POISON.getInterval(level) * 1000));
            poisonTicks.put(player, data);
        }
    }

    private boolean hasPoison(LivingEntity living) {
        return effectManager.hasEffect(living, EffectTypes.POISON);
    }

    @UpdateEvent
    public void tick() {
        final Iterator<Map.Entry<LivingEntity, PoisonData>> iterator = poisonTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, PoisonData> entry = iterator.next();
            LivingEntity damagee = entry.getKey();
            if (!damagee.isValid()) {
                iterator.remove();
                continue;
            }

            if (!hasPoison(damagee)) {
                iterator.remove();
                continue;
            }

            PoisonData data = entry.getValue();
            final long amplifier = data.getAmplifier();
            if (System.currentTimeMillis() >= data.getNextTick() - 50) {
                final long interval = (long) (EffectTypes.POISON.getInterval(amplifier) * 1000);
                data.setNextTick(System.currentTimeMillis() + interval);

                if (damagee.getHealth() <= 2) {
                    return;
                }

                final Effect poison = effectManager.getEffect(damagee, EffectTypes.POISON).orElse(null);
                if (poison == null) {
                    return;
                }

                double damage = EffectTypes.POISON.getDamage(amplifier);
                if (damagee.getHealth() - damage < 2) {
                    //set damage to make the final damage leave the player at 2 health
                    damage = damagee.getHealth() - 2;
                } else {
                    damage = Math.min(damagee.getHealth(), damage);
                }

                UtilDamage.doDamage(new DamageEvent(
                        damagee,
                        poison.getApplier().get(),
                        null,
                        new VanillaDamageCause(EntityDamageEvent.DamageCause.POISON, TriState.NOT_SET, -Long.MAX_VALUE),
                        damage
                ));
            }
        }
    }

    @EventHandler
    public void onPoisonDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
            event.setCancelled(true);
        }
    }

    @Data
    @AllArgsConstructor
    private static class PoisonData {
        long amplifier;
        long nextTick;
    }

}
