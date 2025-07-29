package me.mykindos.betterpvp.core.combat.damage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a collection of damage modifiers and provides methods to apply them.
 */
public class DamageModifiers {

    /**
     * Organized storage for modifiers: ModifierType -> (ModifierOperation, ModifierValue) -> List of modifiers
     */
    private final Map<ModifierType, Map<Map.Entry<ModifierOperation, ModifierValue>, List<DamageModifier>>> modifiersMap = new EnumMap<>(ModifierType.class);

    /**
     * Adds a modifier to the collection.
     *
     * @param type      The type of modifier
     * @param value     The value of the modifier (should be positive)
     * @param source    The source of the modifier (e.g., skill name)
     * @param valueType How the value should be applied (flat or percentage)
     * @param operation Whether this modifier increases or decreases the value
     * @return This DamageModifiers instance for chaining
     */
    public DamageModifiers addModifier(ModifierType type, double value, String source, ModifierValue valueType, ModifierOperation operation) {
        DamageModifier modifier = new DamageModifier(type, Math.abs(value), source, valueType, operation);

        // Add to the map structure for efficient access
        modifiersMap.computeIfAbsent(type, k -> new HashMap<>())
                .computeIfAbsent(Map.entry(operation, valueType), k -> new ArrayList<>())
                .add(modifier);

        return this;
    }

    /**
     * Gets modifiers of a specific type, operation, and value type.
     *
     * @param type      The type of modifiers
     * @param operation The operation of modifiers
     * @param valueType The value type of modifiers
     * @return A list of matching modifiers
     */
    private List<DamageModifier> getModifiers(ModifierType type, ModifierOperation operation, ModifierValue valueType) {
        Map<Map.Entry<ModifierOperation, ModifierValue>, List<DamageModifier>> typeModifiers = modifiersMap.get(type);
        if (typeModifiers == null) {
            return Collections.emptyList();
        }

        List<DamageModifier> result = typeModifiers.get(Map.entry(operation, valueType));
        return result != null ? result : Collections.emptyList();
    }


    /**
     * Applies all relevant modifiers of the specified type to the given base value.
     * Modifiers include both flat and percentage-based adjustments and may increase or
     * decrease the base value accordingly. The final value is guaranteed to be non-negative.
     *
     * @param type      The type of modifiers to apply (e.g., DAMAGE, DAMAGE_DELAY)
     * @param baseValue The initial value to which the modifiers are applied
     * @return The modified value after applying all relevant modifiers, ensuring it is non-negative
     */
    public double applyModifiers(ModifierType type, double baseValue) {
        double result = baseValue;

        // Calculate total INCREASE percentage modifier (additive)
        List<DamageModifier> increasePercentage = getModifiers(type, ModifierOperation.INCREASE, ModifierValue.PERCENTAGE);
        double totalIncreasePercentage = 0;
        for (DamageModifier modifier : increasePercentage) {
            totalIncreasePercentage += modifier.getValue();
        }

        // Apply the total INCREASE percentage modifier additively
        result += (baseValue * totalIncreasePercentage / 100.0);

        // Apply all INCREASE flat modifiers
        List<DamageModifier> increaseFlat = getModifiers(type, ModifierOperation.INCREASE, ModifierValue.FLAT);
        for (DamageModifier modifier : increaseFlat) {
            result += modifier.getValue();
        }

        // Then apply all DECREASE flat modifiers
        List<DamageModifier> decreaseFlat = getModifiers(type, ModifierOperation.DECREASE, ModifierValue.FLAT);
        for (DamageModifier modifier : decreaseFlat) {
            result -= modifier.getValue();
        }

        // Apply DECREASE percentage (multiplicative) - FIXED
        List<DamageModifier> decreasePercentage = getModifiers(type, ModifierOperation.DECREASE, ModifierValue.PERCENTAGE);
        for (DamageModifier modifier : decreasePercentage) {
            result *= (100 - modifier.getValue()) / 100.0; // Multiply by (100 - percentage) / 100
        }

        return Math.max(0, result); // Ensure the result is not negative
    }

    /**
     * Clears all modifiers.
     */
    public void clear() {
        modifiersMap.clear();
    }
}