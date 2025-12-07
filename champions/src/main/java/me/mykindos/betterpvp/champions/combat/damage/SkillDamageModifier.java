package me.mykindos.betterpvp.champions.combat.damage;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;

/**
 * Base class for skill-based damage modifiers
 * Extend this class to create specific skill damage amplifications
 */
public class SkillDamageModifier implements DamageModifier {
    
    protected final Skill skill;
    protected final DamageOperator operator;
    protected final double operand;
    protected final int priority;
    
    /**
     * Creates a skill damage modifier
     * @param skill the skill this modifier is for
     * @param operator the damage operator
     * @param operand the damage operand
     * @param priority the priority of this modifier
     */
    public SkillDamageModifier(Skill skill, DamageOperator operator, double operand, int priority) {
        this.skill = skill;
        this.operator = operator;
        this.priority = priority;
        this.operand = operand;
    }
    
    /**
     * Creates a skill damage modifier with default priority
     * @param skill the skill this modifier is for
     * @param operator the damage operator
     * @param operand the flat damage increase
     */
    public SkillDamageModifier(Skill skill, DamageOperator operator, double operand) {
        this(skill, operator, operand, 200);
    }
    
    @Override
    public String getName() {
        return UtilFormat.cleanString(skill.getName().toLowerCase());
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
        return new ModifierResult(operand, operator, skill.getName());
    }
    
    @Override
    public ModifierType getType() {
        return ModifierType.ABILITY;
    }

    public static class Flat extends SkillDamageModifier {
        public Flat(Skill skill, double damageIncrease, int priority) {
            super(skill, DamageOperator.FLAT, damageIncrease, priority);
        }
        public Flat(Skill skill, double damageIncrease) {
            super(skill, DamageOperator.FLAT, damageIncrease);
        }
    }

    public static class Multiplier extends SkillDamageModifier {
        public Multiplier(Skill skill, double damageMultiplier, int priority) {
            super(skill, DamageOperator.MULTIPLIER, damageMultiplier, priority);
        }
        public Multiplier(Skill skill, double damageMultiplier) {
            super(skill, DamageOperator.MULTIPLIER, damageMultiplier);
        }
    }

}
