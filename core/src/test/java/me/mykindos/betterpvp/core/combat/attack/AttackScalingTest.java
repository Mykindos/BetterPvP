package me.mykindos.betterpvp.core.combat.attack;

import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Attack strength scaling")
class AttackScalingTest {

    private static final double DELTA = 1e-9;

    @Test
    @DisplayName("Base damage follows 0.2 + 0.8p^2")
    void baseDamageCurve() {
        assertEquals(0.2, AttackScaling.BASE.multiplier(0.0, false), DELTA);
        assertEquals(0.4, AttackScaling.BASE.multiplier(0.5, false), DELTA);
        assertEquals(0.848, AttackScaling.BASE.multiplier(0.9, false), DELTA);
        assertEquals(1.0, AttackScaling.BASE.multiplier(1.0, false), DELTA);
    }

    @Test
    @DisplayName("A critical hit multiplies the base curve by 1.5")
    void criticalStacksWithTheBaseCurve() {
        assertEquals(1.5, AttackScaling.BASE.multiplier(1.0, true), DELTA);
        assertEquals(1.272, AttackScaling.BASE.multiplier(0.9, true), DELTA);
    }

    @Test
    @DisplayName("Enchantment damage scales linearly and ignores critical hits")
    void enchantmentDamageIsLinear() {
        assertEquals(0.0, AttackScaling.ENCHANTMENT.multiplier(0.0, false), DELTA);
        assertEquals(0.5, AttackScaling.ENCHANTMENT.multiplier(0.5, false), DELTA);
        assertEquals(1.0, AttackScaling.ENCHANTMENT.multiplier(1.0, false), DELTA);
        assertEquals(0.5, AttackScaling.ENCHANTMENT.multiplier(0.5, true), DELTA);
    }

    @Test
    @DisplayName("Additional damage is untouched by charge or critical hits")
    void additionalDamageIsUnscaled() {
        assertEquals(1.0, AttackScaling.ADDITIONAL.multiplier(0.0, false), DELTA);
        assertEquals(1.0, AttackScaling.ADDITIONAL.multiplier(0.3, true), DELTA);
    }

    @Test
    @DisplayName("Modifier types map onto the buckets they behave like in vanilla")
    void modifierTypesMapToBuckets() {
        assertEquals(AttackScaling.BASE, AttackScaling.forType(ModifierType.STAT));
        assertEquals(AttackScaling.BASE, AttackScaling.forType(ModifierType.EFFECT));
        assertEquals(AttackScaling.BASE, AttackScaling.forType(ModifierType.WEAPON));
        assertEquals(AttackScaling.ENCHANTMENT, AttackScaling.forType(ModifierType.RUNE));
        assertEquals(AttackScaling.ADDITIONAL, AttackScaling.forType(ModifierType.ABILITY));
        assertEquals(AttackScaling.ADDITIONAL, AttackScaling.forType(ModifierType.GENERIC));
        assertEquals(AttackScaling.ADDITIONAL, AttackScaling.forType(ModifierType.ENVIRONMENTAL));
    }
}
