package me.mykindos.betterpvp.core.combat.attack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.config.Config;

/**
 * Toggles and constants for the melee attack cooldown model.
 * <p>
 * With {@link #attackCooldownEnabled} off, players keep an absurdly high {@code ATTACK_SPEED} base value, which
 * suppresses the client attack indicator entirely and keeps every swing at full strength - the pre-cooldown
 * behaviour. With it on, players run at {@link #BASE_ATTACK_SPEED} and damage scales with attack strength.
 */
@Singleton
@Getter
public class MeleeCombatSettings {

    /**
     * Attack speed, in attacks per second, of a player holding a weapon with no attack speed stat.
     * Derived from {@link DamageCause#DEFAULT_DELAY} so the client indicator fills exactly as the melee
     * damage delay expires, rather than at vanilla's faster 4.0.
     */
    public static final double BASE_ATTACK_SPEED = 1000d / DamageCause.DEFAULT_DELAY;

    /**
     * Attack speed applied when the cooldown model is disabled. Anything this large keeps attack strength
     * pinned at 1.0 and force-removes the client's attack indicator.
     */
    public static final double UNCAPPED_ATTACK_SPEED = 100000000d;

    /**
     * Minimum attack strength for a hit to be eligible for a critical hit or a sweep attack.
     */
    public static final double STRONG_ATTACK_THRESHOLD = 0.9d;

    @Inject
    @Config(path = "pvp.attackCooldown.enabled", defaultValue = "true")
    private boolean attackCooldownEnabled;

    @Inject
    @Config(path = "pvp.criticalHits.enabled", defaultValue = "true")
    private boolean criticalHitsEnabled;

    /**
     * The attack speed a player should have as their attribute base value under the current settings.
     */
    public double getPlayerBaseAttackSpeed() {
        return attackCooldownEnabled ? BASE_ATTACK_SPEED : UNCAPPED_ATTACK_SPEED;
    }
}
