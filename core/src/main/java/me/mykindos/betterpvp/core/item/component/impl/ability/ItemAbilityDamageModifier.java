package me.mykindos.betterpvp.core.item.component.impl.ability;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;

/**
 * Base class for item ability-based damage modifiers
 * Extend this class to create specific item ability damage amplifications
 */
public class ItemAbilityDamageModifier implements DamageModifier {
    
    protected final ItemAbility ability;
    protected final DamageOperator operator;
    protected final double operand;
    protected final int priority;
    
    /**
     * Creates an item ability damage modifier
     * @param ability the item ability this modifier is for
     * @param operator the damage operator
     * @param operand the damage operand
     * @param priority the priority of this modifier
     */
    public ItemAbilityDamageModifier(ItemAbility ability, DamageOperator operator, double operand, int priority) {
        this.ability = ability;
        this.operator = operator;
        this.priority = priority;
        this.operand = operand;
    }
    
    /**
     * Creates a item ability damage modifier with default priority
     * @param ability the item ability this modifier is for
    * @param operator the damage operator
    * @param operand the damage operand
     */
    public ItemAbilityDamageModifier(ItemAbility ability, DamageOperator operator, double operand) {
        this(ability, operator, operand, 300);
    }
    
    @Override
    public String getName() {
        return ability.getName();
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
        return new ModifierResult(operand, operator, ability.getName());
    }
    
    @Override
    public ModifierType getType() {
        return ModifierType.ABILITY;
    }

    public static class Flat extends ItemAbilityDamageModifier {
        public Flat(ItemAbility itemAbility, double damageIncrease, int priority) {
            super(itemAbility, DamageOperator.FLAT, damageIncrease, priority);
        }
        public Flat(ItemAbility itemAbility, double damageIncrease) {
            super(itemAbility, DamageOperator.FLAT, damageIncrease);
        }
    }

    public static class Multiplier extends ItemAbilityDamageModifier {
        public Multiplier(ItemAbility itemAbility, double damageMultiplier, int priority) {
            super(itemAbility, DamageOperator.MULTIPLIER, damageMultiplier, priority);
        }
        public Multiplier(ItemAbility itemAbility, double damageMultiplier) {
            super(itemAbility, DamageOperator.MULTIPLIER, damageMultiplier);
        }
    }

}
