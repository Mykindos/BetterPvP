package me.mykindos.betterpvp.core.combat.modifiers;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;

/**
 * Interface for all damage modifiers that can affect damage values
 */
public interface DamageModifier {
    
    /**
     * Gets the unique name of this modifier for identification
     * @return the modifier name
     */
    String getName();
    
    /**
     * Gets the priority of this modifier (higher values execute first)
     * @return the priority value
     */
    int getPriority();
    
    /**
     * Checks if this modifier can be applied to the given damage event
     * @param event the damage event
     * @return true if this modifier should be applied
     */
    boolean canApply(DamageEvent event);
    
    /**
     * Applies this modifier to the damage event. <b>This implementation should
     * not directly modify the event's damage</b>, but return a ModifierResult
     * indicating how the damage should be changed.
     *
     * @param event the damage event to generate a result for.
     * @return the result of applying this modifier
     */
    ModifierResult apply(DamageEvent event);
    
    /**
     * Gets the type of this modifier for categorization
     * @return the modifier type
     */
    ModifierType getType();
}
