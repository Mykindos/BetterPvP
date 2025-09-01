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
     * Multiplier to apply to the damage (1.0 = no change, 1.5 = 50% increase, 0.5 = 50% reduction)
     */
    private final double damageMultiplier;
    
    /**
     * Flat amount to add to the damage (can be negative for reduction)
     */
    private final double damageAddition;
    
    /**
     * Whether this modifier should cancel all other modifiers of lower priority
     * Used for true damage scenarios
     */
    private final boolean cancelOtherModifiers;
    
    /**
     * Reason for this modification (used in damage logs and debug info)
     */
    private final String reason;
    
    /**
     * Creates a simple multiplier-based result
     * @param multiplier the damage multiplier
     * @param reason the reason for the modification
     */
    public ModifierResult(double multiplier, String reason) {
        this(multiplier, 0.0, false, reason);
    }
    
    /**
     * Creates a result that cancels other modifiers (for true damage)
     * @param reason the reason for the modification
     */
    public static ModifierResult cancelOthers(String reason) {
        return new ModifierResult(1.0, 0.0, true, reason);
    }
    
    /**
     * Creates a no-change result
     * @param reason the reason for no modification
     */
    public static ModifierResult noChange(String reason) {
        return new ModifierResult(1.0, 0.0, false, reason);
    }
}
