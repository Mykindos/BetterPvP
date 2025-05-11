package me.mykindos.betterpvp.core.effects.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@BPvPListener
public class EffectListener implements Listener {

    private final Core core;
    private final EffectManager effectManager;


    @Inject
    public EffectListener(Core core, EffectManager effectManager) {
        this.core = core;
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        effectManager.removeAllEffects(event.getEntity());
    }

    @UpdateEvent(priority = 999)
    public void onUpdate() {

        effectManager.getObjects().forEach((effectType, effects) -> {
            effects.values().forEach(value -> {
                Lock lock = new ReentrantLock();
                try {
                    lock.lock();
                    ListIterator<Effect> iterator = value.listIterator();
                    while (iterator.hasNext()) {
                        Effect effect = iterator.next();
                        Entity entity = Bukkit.getEntity(UUID.fromString(effect.getUuid()));
                        if (effect.hasExpired() && !effect.isPermanent()) {
                            if (entity instanceof LivingEntity livingEntity) {
                                UtilServer.callEvent(new EffectExpireEvent(livingEntity, effect, true));
                            }
                            iterator.remove();
                        } else if (entity instanceof LivingEntity livingEntity) {
                            if (effect.getRemovalPredicate() != null && effect.getRemovalPredicate().test(livingEntity) && effect.getLength() - System.currentTimeMillis() < 0) {
                                effect.setLength(0); // Expires next tick to allow damage events and such to be called before removing the effect
                                effect.setPermanent(false);
                            } else if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                                vanillaEffectType.checkActive(livingEntity, effect);
                            }
                            effect.getEffectType().onTick(livingEntity, effect);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            });

        });

        effectManager.getObjects().entrySet().removeIf(entry -> entry.getValue().isEmpty());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExpire(EffectExpireEvent event) {
        Effect effect = event.getEffect();
        effect.getEffectType().onExpire(event.getTarget(), effect, event.isNotify());
    }

    @EventHandler
    public void onEventClear(EffectClearEvent event) {
        effectManager.removeNegativeEffects(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UtilServer.runTaskLater(core, () -> {
            if (effectManager.hasEffect(event.getPlayer(), EffectTypes.HEALTH_BOOST)) {
                event.getPlayer().setHealth(UtilPlayer.getMaxHealth(event.getPlayer()));
            }
        }, 1);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (PotionEffect potionEffect : event.getPlayer().getActivePotionEffects()) {
            event.getPlayer().removePotionEffect(potionEffect.getType());
        }
        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = effectManager.getObject(event.getPlayer().getUniqueId());

        effectsOptional.ifPresent(effects -> {
            effects.values().forEach(effectList -> effectList.forEach(effect -> effect.getEffectType().onReceive(event.getPlayer(), effect)));
        });
    }

    @EventHandler
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if (!event.isAllowed()) return;

        if (effectManager.hasEffect(event.getDamagee(), EffectTypes.PROTECTION)
                || effectManager.hasEffect(event.getDamagee(), EffectTypes.INVISIBILITY)
                || effectManager.hasEffect(event.getDamagee(), EffectTypes.FROZEN)) {
            event.setResult(Event.Result.DENY);
        }
    }

}
