package me.mykindos.betterpvp.core.combat.attack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Decides whether a melee hit is a critical hit, and gives it the feedback vanilla would.
 * <p>
 * Critical hits are ours to own because {@code disablePlayerCrits} is set on every world - without that the
 * server would apply its own 1.5x on top of the one the damage pipeline applies. That flag also suppresses
 * the particles and sound, so those are replayed here.
 */
@Singleton
public class CriticalHitService {

    private final MeleeCombatSettings settings;

    @Inject
    private CriticalHitService(MeleeCombatSettings settings) {
        this.settings = settings;
    }

    /**
     * Evaluates the critical hit conditions against a damage event whose attack strength has already been
     * stamped on it.
     *
     * @param event the damage event
     * @return true if this hit is a critical hit
     */
    public boolean isCritical(DamageEvent event) {
        if (!settings.isCriticalHitsEnabled()) {
            return false;
        }

        if (!(event.getDamager() instanceof Player player)
                || !event.getCause().getCategories().contains(DamageCauseCategory.MELEE)
                || event.isProjectile()
                || !event.isDamageeLiving()) {
            return false;
        }

        if (event.getAttackStrengthScale() <= MeleeCombatSettings.STRONG_ATTACK_THRESHOLD) {
            return false;
        }

        return player.getFallDistance() > 0
                && !player.isOnGround()
                && !player.isSprinting()
                && !player.isInWater()
                && !player.isClimbing()
                && !player.isInsideVehicle()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS)
                && !player.hasPotionEffect(PotionEffectType.SLOW_FALLING)
                && !player.hasPotionEffect(PotionEffectType.LEVITATION);
    }

    /**
     * Plays the critical hit particles and sound on a hit that landed.
     *
     * @param event the damage event
     */
    public void playEffects(DamageEvent event) {
        final Entity damagee = event.getDamagee();
        damagee.getWorld().spawnParticle(Particle.CRIT,
                damagee.getLocation().add(0, damagee.getHeight() * 0.5, 0),
                10, 0.3, 0.3, 0.3, 0.2);

        if (event.getDamager() instanceof Player player) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }
    }
}
