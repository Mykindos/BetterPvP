package me.mykindos.betterpvp.core.effects.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
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
        // Process each effect type and its effects, and clean up both levels in a single pass
        effectManager.getObjects().entrySet().removeIf(entry -> {
            // Process inner map and remove empty lists
            entry.getValue().entrySet().removeIf(innerEntry -> {
                List<Effect> effects = innerEntry.getValue();
                processEffectsForEntity(effects);
                return effects.isEmpty();
            });

            // Return true to remove the outer entry if its inner map is now empty
            return entry.getValue().isEmpty();
        });

    }

    /**
     * Processes all effects for a specific entity
     */
    private void processEffectsForEntity(List<Effect> effects) {

        // Sort effects by amplifier in descending order
        effects.sort((e1, e2) -> Integer.compare(e2.getAmplifier(), e1.getAmplifier()));

        boolean hasTicked = false;
        ListIterator<Effect> iterator = effects.listIterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            Entity entity = Bukkit.getEntity(UUID.fromString(effect.getUuid()));

            if (entity instanceof LivingEntity livingEntity) {
                if (isExpiredEffect(effect)) {
                    handleExpiredEffect(livingEntity, effect, iterator);
                } else {
                    if (!hasTicked) {
                        updateActiveEffect(livingEntity, effect);

                        // Perform effect tick on the highest amplifier
                        effect.getEffectType().onTick(livingEntity, effect);
                        hasTicked = true;
                    }
                }
            } else if (isExpiredEffect(effect)) {
                iterator.remove();
            }
        }

    }

    /**
     * Determines if an effect has expired and should be removed
     */
    private boolean isExpiredEffect(Effect effect) {
        return effect.hasExpired() && !effect.isPermanent();
    }

    /**
     * Handles an expired effect - triggers event and removes it
     */
    private void handleExpiredEffect(LivingEntity livingEntity, Effect effect, ListIterator<Effect> iterator) {
        UtilServer.callEvent(new EffectExpireEvent(livingEntity, effect, true));
        iterator.remove();
    }

    /**
     * Updates and applies an active effect
     */
    private void updateActiveEffect(LivingEntity livingEntity, Effect effect) {
        // Check if effect should be removed based on predicate
        if (shouldRemoveByPredicate(effect, livingEntity)) {
            effect.setLength(0);
            effect.setPermanent(false);
        }
        // Check vanilla effect type
        else if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
            vanillaEffectType.checkActive(livingEntity, effect);
        }
    }

    /**
     * Determines if an effect should be removed based on its removal predicate
     */
    private boolean shouldRemoveByPredicate(Effect effect, LivingEntity livingEntity) {
        return effect.getRemovalPredicate() != null
                && effect.getRemovalPredicate().test(livingEntity)
                && effect.getLength() - System.currentTimeMillis() < 0;
    }

    /**
     * Removes any effect types that have no active effects
     */
    private void cleanupEmptyEffectTypes() {
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
        Optional<ConcurrentHashMap<String, List<Effect>>> effectsOptional = effectManager.getObject(event.getPlayer().getUniqueId().toString());

        effectsOptional.ifPresent(effects -> {
            effects.values().forEach(effectList -> effectList.forEach(effect -> effect.getEffectType().onReceive(event.getPlayer(), effect)));
        });
    }

    @EventHandler
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if (!event.isAllowed()) return;

        if (effectManager.hasEffect(event.getDamagee(), EffectTypes.PROTECTION)
                || effectManager.hasEffect(event.getDamagee(), EffectTypes.VANISH)
                || effectManager.hasEffect(event.getDamagee(), EffectTypes.FROZEN)) {
            event.setResult(Event.Result.DENY);
        }
    }

}
