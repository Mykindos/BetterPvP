package me.mykindos.betterpvp.core.combat.listeners;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.data.SoundProvider;
import me.mykindos.betterpvp.core.combat.delay.DamageDelayManager;
import me.mykindos.betterpvp.core.combat.durability.DurabilityProcessor;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Handles final damage application, effects, and cleanup
 */
@CustomLog
public class DamageEventFinalizer {
    
    private final Core core;
    private final DurabilityProcessor durabilityProcessor;
    private final DamageDelayManager delayManager;
    private final Set<UUID> delayKillSet = new HashSet<>();
    private final ClientManager clientManager;

    @Inject
    private DamageEventFinalizer(Core core, DurabilityProcessor durabilityProcessor, DamageDelayManager delayManager, ClientManager clientManager) {
        this.core = core;
        this.durabilityProcessor = durabilityProcessor;
        this.delayManager = delayManager;
        this.clientManager = clientManager;
    }
    
    protected void finalizeEvent(@NotNull DamageEvent event, @Nullable CustomDamageAdapter adapter) {
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

        if (adapter != null) {
            // Process custom damage adapter
            adapter.processCustomDamageAdapter(event);
        }

        // Play damage effects
        playDamageEffects(event);

        // Apply final damage
        applyFinalDamage(event);

        // Send debug info
        sendDebugInfo(event);

        // Update last damaged
        if (event.getDamagee() instanceof Player player && event.getDamager() != null) {
            clientManager.search().online(player).getGamer().setLastDamaged(System.currentTimeMillis());
        }
        if (event.getDamager() instanceof Player player) {
            final Gamer gamer = clientManager.search().online(player).getGamer();
            gamer.setLastDamaged(System.currentTimeMillis());
            gamer.setLastDealtDamageValue(event.getModifiedDamage());
        }

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

        trajectory.multiply(event.getMultiplier());
        double strength = 0.2D + trajectory.length() * 0.9D;

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
    }
    
    /**
     * Applies the final damage to the entity
     */
    private void applyFinalDamage(DamageEvent event) {
        if (!event.getDamagee().isValid() || !event.isDamageeLiving()
                || Objects.requireNonNull(event.getLivingDamagee()).getHealth() <= 0 || event.getLivingDamagee().isDead()
                || delayKillSet.contains(event.getDamagee().getUniqueId())) {
            return;
        }

        LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        double finalHealth = damagee.getHealth() - event.getModifiedDamage();

        if (finalHealth <= 0.0) {
            // Handle entity death with delay to fix Paper issue
            // Temporary measure to fix https://github.com/PaperMC/Paper/issues/12148
            if (!delayKillSet.contains(damagee.getUniqueId())) {
                delayKillSet.add(damagee.getUniqueId());
                UtilServer.runTaskLater(core, () -> {
                    if (event.getDamager() instanceof Player killer) damagee.setKiller(killer);
                    damagee.setHealth(0);
                    delayKillSet.remove(damagee.getUniqueId());
                }, 1L);
            }
        } else {
            damagee.setHealth(finalHealth);
        }
    }

    private void sendDebugInfo(DamageEvent event) {
        if (event.getDamagee() instanceof Player player && player.isOp() && player.getEquipment().getItemInMainHand().getType() == Material.DEBUG_STICK) {
            double modifiedDamage = event.getModifiedDamage();
            final List<String> categories = new ArrayList<>();
            for (DamageCauseCategory category : event.getCause().getCategories()) {
                categories.add(category.name());
            }

            UtilMessage.simpleMessage(player, "");
            UtilMessage.simpleMessage(player, "Damage", "<u>Details:");
            UtilMessage.simpleMessage(player, "Damage", "Raw Damage: <alt2>" + event.getRawDamage());
            UtilMessage.simpleMessage(player, "Damage", "Damage: <alt2>" + event.getDamage());
            UtilMessage.simpleMessage(player, "Damage", "Final Damage: <alt2>" + modifiedDamage);
            UtilMessage.simpleMessage(player, "Damage", "Projectile: " + (event.isProjectile() ? "<green>Yes" : "<red>No"));
            UtilMessage.simpleMessage(player, "Damage", "Knockback: " + (event.isKnockback() ? "<green>Yes" : "<red>No"));
            UtilMessage.simpleMessage(player, "Damage", "Hurt Animation: " + (event.isHurtAnimation() ? "<green>Yes" : "<red>No"));
            UtilMessage.simpleMessage(player, "Damage", "Living Damagee: " + (event.isDamageeLiving() ? "<green>Yes" : "<red>No"));
            UtilMessage.simpleMessage(player, "Damage", "Damage Delay: <alt2>" + event.getDamageDelay() + "ms");
            UtilMessage.simpleMessage(player, "Damage", "Force Damage Delay: <alt2>" + event.getForceDamageDelay() + "ms");
            UtilMessage.simpleMessage(player, "Damage", "Reasons: <alt2>" + String.join(", ", event.getReasons()));
            UtilMessage.simpleMessage(player, "Damage", "");
            UtilMessage.simpleMessage(player, "Damage", "<u>Cause Breakdown:");
            UtilMessage.simpleMessage(player, "Damage", "Cause: <alt2>" + event.getCause().getDisplayName());
            UtilMessage.simpleMessage(player, "Damage", "Bukkit Cause: <alt2>" + event.getCause().getBukkitCause());
            UtilMessage.simpleMessage(player, "Damage", "Damage Categories: <alt2>" + String.join(", ", categories));
            UtilMessage.simpleMessage(player, "Damage", "True Damage: <alt2>" + event.getCause().isTrueDamage());
            UtilMessage.simpleMessage(player, "Damage", "");
            UtilMessage.simpleMessage(player, "Damage", "<u>Modifiers:");

            final List<DamageModifier> appliedModifiers = event.getAppliedModifiers();
            final Multimap<ModifierType, DamageModifier> modifiers = event.getModifiers();
            for (DamageModifier modifier : appliedModifiers) {
                final ModifierType type = modifier.getType();
                final TextComponent.Builder builder = Component.text();
                builder.append(Component.text("[")).append(Component.text("✔", NamedTextColor.GREEN)).append(Component.text("]"));
                builder.appendSpace();
                builder.append(Component.text("(P: ")).append(Component.text(modifier.getPriority(), NamedTextColor.YELLOW)).append(Component.text(")"));
                builder.appendSpace();
                builder.append(Component.text(modifier.getName()).decorate(TextDecoration.ITALIC));
                builder.appendSpace();
                builder.append(Component.text("(")).append(Component.text(type.name(), NamedTextColor.YELLOW)).append(Component.text("):"));
                builder.appendSpace();
                final ModifierResult result = modifier.apply(event);
                builder.append(Component.text(result.getDamageOperator().name(), NamedTextColor.AQUA));
                builder.appendSpace();
                builder.append(Component.text(result.getDamageOperand(), NamedTextColor.YELLOW));

                player.sendMessage(builder.build());
            }

            for (Map.Entry<ModifierType, DamageModifier> entry : modifiers.entries()) {
                final ModifierType type = entry.getKey();
                final DamageModifier modifier = entry.getValue();
                if (appliedModifiers.contains(modifier)) continue;
                final TextComponent.Builder builder = Component.text();
                builder.append(Component.text("[")).append(Component.text("✘", NamedTextColor.GREEN)).append(Component.text("]"));
                builder.appendSpace();
                builder.append(Component.text("(P: ")).append(Component.text(modifier.getPriority(), NamedTextColor.YELLOW)).append(Component.text(")"));
                builder.appendSpace();
                builder.append(Component.text(modifier.getName()).decorate(TextDecoration.ITALIC));
                builder.appendSpace();
                builder.append(Component.text("(")).append(Component.text(type.name(), NamedTextColor.YELLOW)).append(Component.text(")"));

                player.sendMessage(builder.build());
            }
            UtilMessage.simpleMessage(player, "");
        }
    }
}
