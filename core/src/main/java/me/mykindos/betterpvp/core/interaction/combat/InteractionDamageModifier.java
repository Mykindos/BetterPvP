package me.mykindos.betterpvp.core.interaction.combat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.interaction.Interaction;

/**
 * Base class for item interaction-based damage modifiers
 * Extend this class to create specific item interaction damage amplifications
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InteractionDamageModifier implements DamageModifier {
    
    protected final Interaction interaction;
    @Getter
    protected final DamageOperator damageOperator;
    @With
    protected ModifierType modifierType = ModifierType.ABILITY;
    @Getter
    protected final double damageOperand;
    protected final int priority;
    
    /**
     * Creates an item interaction damage modifier
     * @param interaction the item interaction this modifier is for
     * @param damageOperator the damage operator
     * @param damageOperand the damage operand
     * @param priority the priority of this modifier
     */
    public InteractionDamageModifier(Interaction interaction, DamageOperator damageOperator, double damageOperand, int priority) {
        this.interaction = interaction;
        this.damageOperator = damageOperator;
        this.priority = priority;
        this.damageOperand = damageOperand;
    }
    
    /**
     * Creates a item interaction damage modifier with default priority
     * @param interaction the item interaction this modifier is for
    * @param damageOperator the damage operator
    * @param damageOperand the damage operand
     */
    public InteractionDamageModifier(Interaction interaction, DamageOperator damageOperator, double damageOperand) {
        this(interaction, damageOperator, damageOperand, 300);
    }
    
    @Override
    public String getName() {
        return interaction.getName();
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
        return new ModifierResult(damageOperand, damageOperator, interaction.getName());
    }
    
    @Override
    public ModifierType getType() {
        return modifierType;
    }

    public static class Flat extends InteractionDamageModifier {
        public Flat(Interaction iteminteraction, double damageIncrease, int priority) {
            super(iteminteraction, DamageOperator.FLAT, damageIncrease, priority);
        }
        public Flat(Interaction iteminteraction, double damageIncrease) {
            super(iteminteraction, DamageOperator.FLAT, damageIncrease);
        }
    }

    public static class Multiplier extends InteractionDamageModifier {
        public Multiplier(Interaction iteminteraction, double damageMultiplier, int priority) {
            super(iteminteraction, DamageOperator.MULTIPLIER, damageMultiplier, priority);
        }
        public Multiplier(Interaction iteminteraction, double damageMultiplier) {
            super(iteminteraction, DamageOperator.MULTIPLIER, damageMultiplier);
        }
    }

}
