package me.mykindos.betterpvp.core.combat.damage;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a single damage modifier that can be applied to a damage event.
 */
@Data
@AllArgsConstructor
public class DamageModifier {

    /**
     * The type of modifier
     */
    private final ModifierType type;

    /**
     * The value of the modifier
     */
    private final double value;

    /**
     * The source of the modifier (e.g., skill name)
     */
    private final String source;

    /**
     * How the value should be applied (flat or percentage)
     */
    private final ModifierValue valueType;

    /**
     * Whether this modifier increases or decreases the value
     */
    private final ModifierOperation operation;
}
