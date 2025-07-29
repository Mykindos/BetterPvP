package me.mykindos.betterpvp.core.combat.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageModifiersTest {

    /**
     * This class tests the `applyModifiers` method of the DamageModifiers class.
     * <p>
     * The `applyModifiers` method applies a series of modifiers (increasing or decreasing percentages or flat values)
     * to a base value and computes the resultant value. The method ensures the result is never less than zero.
     */

    @Test
    void testApplyModifiers_WithNoModifiers_BaseValueReturned() {
        DamageModifiers damageModifiers = new DamageModifiers();
        double baseValue = 100.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(100.0, result, "Result should equal the baseValue as no modifiers are applied.");
    }

    @Test
    void testApplyModifiers_WithIncreasePercentageModifier() {
        DamageModifiers damageModifiers = new DamageModifiers();
        damageModifiers.addModifier(ModifierType.DAMAGE, 20, "TestSource", ModifierValue.PERCENTAGE, ModifierOperation.INCREASE);
        double baseValue = 100.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(120.0, result, "Result should be increased by 20% of the base value.");
    }

    @Test
    void testApplyModifiers_WithIncreaseFlatModifier() {
        DamageModifiers damageModifiers = new DamageModifiers();
        damageModifiers.addModifier(ModifierType.DAMAGE, 15, "TestSource", ModifierValue.FLAT, ModifierOperation.INCREASE);
        double baseValue = 100.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(115.0, result, "Result should be increased by a flat value of 15.");
    }

    @Test
    void testApplyModifiers_WithDecreaseFlatModifier() {
        DamageModifiers damageModifiers = new DamageModifiers();
        damageModifiers.addModifier(ModifierType.DAMAGE, 10, "TestSource", ModifierValue.FLAT, ModifierOperation.DECREASE);
        double baseValue = 100.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(90.0, result, "Result should be decreased by a flat value of 10.");
    }

    @Test
    void testApplyModifiers_WithDecreasePercentageModifier() {
        DamageModifiers damageModifiers = new DamageModifiers();
        damageModifiers.addModifier(ModifierType.DAMAGE, 25, "TestSource", ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);
        double baseValue = 100.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(75.0, result, "Result should be decreased by 25% of the base value.");
    }

    @Test
    void testApplyModifiers_WithMixedModifiers() {
        DamageModifiers damageModifiers = new DamageModifiers();
        damageModifiers.addModifier(ModifierType.DAMAGE, 20, "TestSource1", ModifierValue.PERCENTAGE, ModifierOperation.INCREASE);
        damageModifiers.addModifier(ModifierType.DAMAGE, 10, "TestSource2", ModifierValue.FLAT, ModifierOperation.INCREASE);
        damageModifiers.addModifier(ModifierType.DAMAGE, 5, "TestSource3", ModifierValue.FLAT, ModifierOperation.DECREASE);
        damageModifiers.addModifier(ModifierType.DAMAGE, 10, "TestSource4", ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);
        double baseValue = 200.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(220.5, result, "Result should account for all increases and decreases combined.");
    }

    @Test
    void testApplyModifiers_ResultNeverNegative() {
        DamageModifiers damageModifiers = new DamageModifiers();
        damageModifiers.addModifier(ModifierType.DAMAGE, 200, "TestSource", ModifierValue.FLAT, ModifierOperation.DECREASE);
        double baseValue = 100.0;

        double result = damageModifiers.applyModifiers(ModifierType.DAMAGE, baseValue);

        assertEquals(0.0, result, "Result should never be negative, even if the modifiers reduce it below zero.");
    }
}