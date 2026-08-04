package me.mykindos.betterpvp.core.combat.attack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves how charged a melee swing was.
 * <p>
 * The ticker itself is vanilla's: it resets on every attack, block break and main-hand item switch, and
 * refills over {@code 20 / ATTACK_SPEED} ticks. Reading it back rather than tracking our own means the
 * client's attack indicator and the damage it produces are driven by the same number.
 */
@Singleton
public class AttackChargeService {

    private final MeleeCombatSettings settings;

    @Inject
    private AttackChargeService(MeleeCombatSettings settings) {
        this.settings = settings;
    }

    /**
     * Gets the attack strength completion for a damager, 0 to 1.
     * <p>
     * Anything without a swing ticker - mobs, and every player while the cooldown model is off - hits at
     * full strength.
     *
     * @param damager the entity that swung, may be null
     * @return the attack strength completion
     */
    public double getCharge(@Nullable LivingEntity damager) {
        if (!settings.isAttackCooldownEnabled() || !(damager instanceof Player player)) {
            return 1.0;
        }

        return player.getAttackCooldown();
    }
}
