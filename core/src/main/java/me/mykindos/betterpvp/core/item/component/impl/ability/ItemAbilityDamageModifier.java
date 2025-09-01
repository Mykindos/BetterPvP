package me.mykindos.betterpvp.core.item.component.impl.ability;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;

/**
 * Base class for item ability-based damage modifiers
 * Extend this class to create specific item ability damage amplifications
 */
public class ItemAbilityDamageModifier implements DamageModifier {
    
    protected final ItemAbility ability;
    protected final double damageMultiplier;
    protected final double damageIncrease;
    protected final int priority;
    
    /**
     * Creates an item ability damage modifier
     * @param ability the item ability this modifier is for
     * @param damageMultiplier the damage multiplier (1.0 = no change, 1.5 = 50% increase)
     * @param damageIncrease the flat damage increase
     * @param priority the priority of this modifier
     */
    public ItemAbilityDamageModifier(ItemAbility ability, double damageMultiplier, double damageIncrease, int priority) {
        this.ability = ability;
        this.damageMultiplier = damageMultiplier;
        this.priority = priority;
        this.damageIncrease = damageIncrease;
    }
    
    /**
     * Creates a item ability damage modifier with default priority
     * @param ability the item ability this modifier is for
     * @param damageMultiplier the damage multiplier
     * @param damageIncrease the flat damage increase
     */
    public ItemAbilityDamageModifier(ItemAbility ability, double damageMultiplier, double damageIncrease) {
        this(ability, damageMultiplier, damageIncrease, 200);
    }
    
    @Override
    public String getName() {
        return ability.getName().toLowerCase().replace(" ", "_");
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public boolean canApply(DamageEvent event) {
        // Allow subclasses to define custom logic
        return canApplyCustom(event);
    }
    
    /**
     * Custom logic for determining if this modifier can apply
     * Override this in subclasses for more complex conditions
     * @param event the damage event
     * @return true if this modifier should apply
     */
    protected boolean canApplyCustom(DamageEvent event) {
        return true;
    }
    
    @Override
    public ModifierResult apply(DamageEvent event) {
        return new ModifierResult(damageMultiplier, damageIncrease, false, ability.getName());
    }
    
    @Override
    public ModifierType getType() {
        return ModifierType.ABILITY;
    }

    public static class Flat extends ItemAbilityDamageModifier {
        public Flat(ItemAbility itemAbility, double damageIncrease, int priority) {
            super(itemAbility, 1.0, damageIncrease, priority);
        }
        public Flat(ItemAbility itemAbility, double damageIncrease) {
            super(itemAbility, 1.0, damageIncrease);
        }
    }

    public static class Multiplier extends ItemAbilityDamageModifier {
        public Multiplier(ItemAbility itemAbility, double damageMultiplier, int priority) {
            super(itemAbility, damageMultiplier, 0.0, priority);
        }
        public Multiplier(ItemAbility itemAbility, double damageMultiplier) {
            super(itemAbility, damageMultiplier, 0.0);
        }
    }

}
