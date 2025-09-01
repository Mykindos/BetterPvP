package me.mykindos.betterpvp.core.combat.delay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages damage delays to prevent spam damage
 */
@Singleton
@CustomLog
public class DamageDelayManager {
    
    private final EffectManager effectManager;
    private final Map<DelayKey, DelayData> activeDelays = new ConcurrentHashMap<>();
    
    @Inject
    public DamageDelayManager(EffectManager effectManager) {
        this.effectManager = effectManager;
    }
    
    /**
     * Checks if an entity can deal damage to another entity
     * @param damager the entity dealing damage (can be null for environmental damage)
     * @param damagee the entity taking damage
     * @param cause the damage cause
     * @return true if damage can be dealt
     */
    public boolean canDealDamage(@Nullable Entity damager, Entity damagee, DamageCause cause) {
        DelayKey key = new DelayKey(
            damagee.getUniqueId(),
            damager != null ? damager.getUniqueId() : null,
            cause
        );
        
        DelayData delayData = activeDelays.get(key);
        if (delayData == null) {
            return true;
        }
        
        return UtilTime.elapsed(delayData.getTimestamp(), delayData.getDuration());
    }
    
    /**
     * Adds a damage delay for the specified entities and cause
     * @param damager the entity dealing damage (can be null)
     * @param damagee the entity taking damage
     * @param cause the damage cause
     * @param duration the delay duration in milliseconds
     */
    public void addDelay(@Nullable Entity damager, Entity damagee, DamageCause cause, long duration) {
        DelayKey key = new DelayKey(
            damagee.getUniqueId(),
            damager != null ? damager.getUniqueId() : null,
            cause
        );
        
        DelayData delayData = new DelayData(System.currentTimeMillis(), duration);
        activeDelays.put(key, delayData);
        
        log.debug("Added damage delay: {} -> {} for {} ms", 
                 damager != null ? damager.getName() : "Environment", 
                 damagee.getName(), duration).submit();
    }
    
    /**
     * Applies effect modifiers to damage delay (attack speed, concussion, etc.)
     * @param event the damage event to modify
     */
    public void applyEffectModifiers(DamageEvent event) {
        if (event.getDamageDelay() <= 0 || event.getDamager() == null) {
            return;
        }
        
        long originalDelay = event.getDamageDelay();

        // Apply attack speed effect (reduces delay)
        effectManager.getEffect(event.getDamager(), EffectTypes.ATTACK_SPEED).ifPresent(effect -> {
            double reduction = effect.getAmplifier() / 100.0;
            long newDelay = (long) (originalDelay * (1 - reduction));
            event.setDamageDelay(newDelay);
        });
        
        // Apply concussion effect (increases delay)
        effectManager.getEffect(event.getDamager(), EffectTypes.CONCUSSED).ifPresent(effect -> {
            LivingEntity concussedPlayer = effect.getApplier();
            if (concussedPlayer != null) {
                concussedPlayer.getWorld().playSound(concussedPlayer.getLocation(), Sound.ENTITY_GOAT_LONG_JUMP, 2.0F, 1.0F);
                
                double increase = effect.getAmplifier() * 0.25;
                long newDelay = (long) (event.getDamageDelay() * (1 + increase));
                event.setDamageDelay(newDelay);
            }
        });
    }
    
    /**
     * Sets default delays based on damage cause
     * @param event the damage event to set delays for
     */
    public void setDefaultDelays(DamageEvent event) {
        if (event.getDamageDelay() != 0) {
            return; // Delay already set
        }
        
        long defaultDelay = event.getCause().getDefaultDelay();
        event.setDamageDelay(defaultDelay);
        
        log.debug("Set default delay of {} ms for cause: {}", defaultDelay, event.getCause().getName()).submit();
    }
    
    /**
     * Processes damage delay for an event
     * @param event the damage event
     * @return true if the damage should proceed, false if it should be blocked by delay
     */
    public boolean processDamageDelay(DamageEvent event) {
        // Check if damage can proceed
        final boolean canDealDamage = canDealDamage(event.getDamager(), event.getDamagee(), event.getCause());
        if (!canDealDamage) {
            return false;
        }

        // Set default delays if not already set
        setDefaultDelays(event);

        // Apply effect modifiers
        applyEffectModifiers(event);

        return true;
    }
    
    /**
     * Gets the remaining delay for a specific damage combination
     * @param damager the damager (can be null)
     * @param damagee the damagee
     * @param cause the damage cause
     * @return the remaining delay in milliseconds, or 0 if no delay
     */
    public long getRemainingDelay(@Nullable Entity damager, Entity damagee, DamageCause cause) {
        DelayKey key = new DelayKey(
            damagee.getUniqueId(),
            damager != null ? damager.getUniqueId() : null,
            cause
        );
        
        DelayData delayData = activeDelays.get(key);
        if (delayData == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - delayData.getTimestamp();
        long remaining = delayData.getDuration() - elapsed;
        
        return Math.max(0, remaining);
    }
    
    /**
     * Clears all delays for a specific entity
     * @param entity the entity to clear delays for
     */
    public void clearDelaysForEntity(Entity entity) {
        UUID entityId = entity.getUniqueId();
        
        activeDelays.entrySet().removeIf(entry -> {
            DelayKey key = entry.getKey();
            return entityId.equals(key.getDamageeId()) || entityId.equals(key.getDamagerId());
        });
        
        log.debug("Cleared all delays for entity: {}", entity.getName()).submit();
    }
    
    /**
     * Cleans up expired delays
     */
    @UpdateEvent
    public void cleanupExpiredDelays() {
        activeDelays.entrySet().removeIf(entry -> {
            DelayData delayData = entry.getValue();
            final Entity damagee = Bukkit.getEntity(entry.getKey().getDamageeId());
            final boolean elapsed = UtilTime.elapsed(delayData.getTimestamp(), delayData.getDuration());
            if (elapsed || damagee == null || !damagee.isValid()) {
                return true;
            }
            return false;
        });
    }
}
