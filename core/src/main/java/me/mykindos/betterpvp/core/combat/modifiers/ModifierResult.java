package me.mykindos.betterpvp.core.combat.modifiers;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Result of applying a damage modifier
 */
@Data
@RequiredArgsConstructor
public class ModifierResult {
    
    /**
     * Damage to be applied
     */
    private final double damageOperand;

    /**
     * Damage operator
     */
    private final DamageOperator damageOperator;
    
    /**
     * Reason for this modification (used in damage logs and debug info)
     */
    private final String reason;

    public boolean isReductive() {
        return switch (damageOperator) {
            case FLAT -> damageOperand <= 0; // 0 is equivalent to no damage, meaning it's a reductive modifier
            case MULTIPLIER -> damageOperand < 1.0;
        };
    }
}
