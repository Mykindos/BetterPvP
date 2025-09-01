package me.mykindos.betterpvp.core.combat.listeners;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.delay.DamageDelayManager;
import me.mykindos.betterpvp.core.combat.durability.DurabilityProcessor;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Handles final damage application, effects, and cleanup
 */
@CustomLog
public class DamageEventFinalizer implements Listener {
    
    private final Core core;
    private final DurabilityProcessor durabilityProcessor;
    private final DamageDelayManager delayManager;
    private final Set<UUID> delayKillSet = new HashSet<>();
    
    @Inject
    private DamageEventFinalizer(Core core, DurabilityProcessor durabilityProcessor, DamageDelayManager delayManager) {
        this.core = core;
        this.durabilityProcessor = durabilityProcessor;
        this.delayManager = delayManager;
    }
    
    protected void finalizeEvent(DamageEvent event) {
        // Skip if damage is 0 or negative
        if (event.getDamage() <= 0) {
            return;
        }
        
        // Apply knockback if enabled
        if (event.isKnockback() && event.getDamager() != null && event.isDamageeLiving()) {
            applyKnockback(event);
        }
        
        // Process durability
        durabilityProcessor.processDurability(event);

        // Process delay
        delayManager.addDelay(event.getDamager(), event.getDamagee(), event.getCause(), event.getDamageDelay());
        
        // Play hit sounds
        playHitSounds(event);
        
        // Play damage effects
        playDamageEffects(event);
        
        // Apply final damage
        applyFinalDamage(event);
        
        log.debug("Finalized damage: {} dealt {} damage to {}", 
                 event.getDamager() != null ? event.getDamager().getName() : "Environment",
                 event.getDamage(), event.getDamagee().getName()).submit();
    }
    
    /**
     * Applies knockback to the damage event
     */
    private void applyKnockback(DamageEvent event) {
        CustomKnockbackEvent knockbackEvent = UtilServer.callEvent(new CustomKnockbackEvent(
                event.getLivingDamagee(), event.getDamager(), event.getDamage(), event));
        
        if (!knockbackEvent.isCancelled()) {
            applyKnockbackVelocity(knockbackEvent);
        }
    }
    
    /**
     * Applies the actual knockback velocity
     */
    private void applyKnockbackVelocity(CustomKnockbackEvent event) {
        double knockback = event.getDamage();
        if (knockback < 2.0D && !event.isCanBypassMinimum()) {
            knockback = 2.0D;
        }
        
        knockback = Math.max(0, Math.log10(knockback));
        if (knockback == 0) return;
        
        Vector trajectory = UtilVelocity.getTrajectory2d(event.getDamager(), event.getDamagee());
        trajectory.multiply(0.6D * knockback);
        trajectory.setY(Math.abs(trajectory.getY()));
        
        // Handle projectile knockback differently
        if (event.getDamageEvent().getProjectile() != null) {
            trajectory = event.getDamageEvent().getProjectile().getVelocity();
            trajectory.setY(0);
            trajectory.multiply(0.37 * knockback / trajectory.length());
            trajectory.setY(0.06);
        }
        
        double strength = 0.2D + trajectory.length() * 0.9D;
        trajectory.multiply(event.getMultiplier());
        
        VelocityData velocityData = new VelocityData(trajectory, strength, false, 0.0D, 
                Math.abs(0.2D * knockback), 0.4D + (0.04D * knockback), true);
        UtilVelocity.velocity(event.getDamagee(), event.getDamager(), velocityData, VelocityType.KNOCKBACK);
    }
    
    /**
     * Plays hit sounds for the damage event
     */
    private void playHitSounds(DamageEvent event) {
        // Play arrow hit sound for projectile damage
        if (event.getDamager() instanceof Player player && event.isProjectile()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
            event.getDamagee().getWorld().playSound(event.getDamagee().getLocation(),
                    Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);
        }
    }
    
    /**
     * Plays damage effects (sounds, animations, etc.)
     */
    private void playDamageEffects(DamageEvent event) {
        if (!event.isDamageeLiving()) {
            return;
        }
        
        LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        
        // Play hurt animation
        if (event.isHurtAnimation()) {
            damagee.playHurtAnimation(270);
        }
        
        // Play damage sound
        SoundProvider provider = event.getSoundProvider();
        net.kyori.adventure.sound.Sound sound = provider.apply(event);
        if (sound != null) {
            if (provider.fromEntity()) {
                damagee.getWorld().playSound(sound, damagee);
            } else {
                damagee.getWorld().playSound(damagee.getLocation(), sound.name().asString(), 
                        sound.volume(), sound.pitch());
            }
        }
        
        // Set damager level to damage amount (for visual feedback)
        if (event.getDamager() instanceof Player player) {
            player.setLevel((int) event.getDamage());
        }
    }
    
    /**
     * Applies the final damage to the entity
     */
    private void applyFinalDamage(DamageEvent event) {
        if (!event.isDamageeLiving()) {
            return;
        }
        
        LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        double finalHealth = damagee.getHealth() - event.getModifiedDamage();
        
        if (finalHealth < 1.0) {
            // Handle entity death with delay to fix Paper issue
            // Temporary measure to fix https://github.com/PaperMC/Paper/issues/12148
            if (!delayKillSet.contains(damagee.getUniqueId())) {
                delayKillSet.add(damagee.getUniqueId());
                UtilServer.runTaskLater(core, () -> {
                    damagee.setHealth(0);
                    delayKillSet.remove(damagee.getUniqueId());
                }, 1L);
            }
        } else {
            damagee.setHealth(finalHealth);
        }
    }
}
