package me.mykindos.betterpvp.core.combat.attack;

import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;

/**
 * How a damage contribution responds to attack strength.
 * <p>
 * Mirrors the three buckets vanilla melee damage falls into: base damage is scaled by
 * {@code 0.2 + 0.8p²} and multiplied again by a critical hit, enchantment damage is scaled linearly by
 * {@code p} and ignores critical hits, and additional damage is untouched by either.
 * <p>
 * This only distinguishes {@link DamageOperator#FLAT} contributions. A multiplier is proportional by
 * definition, so it rides on whatever it multiplies (the base bucket) regardless of its scaling.
 */
public enum AttackScaling {

    /**
     * Weapon damage and anything that behaves like an {@code ATTACK_DAMAGE} attribute modifier would in
     * vanilla - item damage stats, Strength.
     */
    BASE,

    /**
     * Enchantment-like bonuses: runes and gems.
     */
    ENCHANTMENT,

    /**
     * Damage added on top of the attack itself - skills, abilities, environmental sources. Landing a
     * half-charged swing does not shrink these.
     */
    ADDITIONAL;

    /**
     * The bucket a modifier falls into when it does not override {@link DamageModifier#getScaling()}.
     */
    public static AttackScaling forType(ModifierType type) {
        return switch (type) {
            case STAT, EFFECT, WEAPON -> BASE;
            case RUNE -> ENCHANTMENT;
            // ARMOR only ever reduces, and reductive contributions are never scaled at all.
            case ABILITY, GENERIC, ENVIRONMENTAL, ARMOR -> ADDITIONAL;
        };
    }

    /**
     * The multiplier this bucket applies at the given attack strength.
     *
     * @param attackStrength attack strength completion, 0 to 1
     * @param critical       whether the hit is a critical hit
     */
    public double multiplier(double attackStrength, boolean critical) {
        return switch (this) {
            case BASE -> (0.2d + 0.8d * attackStrength * attackStrength) * (critical ? 1.5d : 1.0d);
            case ENCHANTMENT -> attackStrength;
            case ADDITIONAL -> 1.0d;
        };
    }
}
