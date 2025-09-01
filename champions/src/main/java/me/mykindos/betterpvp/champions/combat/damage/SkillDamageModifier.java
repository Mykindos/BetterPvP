package me.mykindos.betterpvp.champions.combat.damage;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;

/**
 * Base class for skill-based damage modifiers
 * Extend this class to create specific skill damage amplifications
 */
public class SkillDamageModifier implements DamageModifier {
    
    protected final Skill skill;
    protected final double damageMultiplier;
    protected final double damageIncrease;
    protected final int priority;
    
    /**
     * Creates a skill damage modifier
     * @param skill the skill this modifier is for
     * @param damageMultiplier the damage multiplier (1.0 = no change, 1.5 = 50% increase)
     * @param damageIncrease the flat damage increase
     * @param priority the priority of this modifier
     */
    public SkillDamageModifier(Skill skill, double damageMultiplier, double damageIncrease, int priority) {
        this.skill = skill;
        this.damageMultiplier = damageMultiplier;
        this.priority = priority;
        this.damageIncrease = damageIncrease;
    }
    
    /**
     * Creates a skill damage modifier with default priority
     * @param skill the skill this modifier is for
     * @param damageMultiplier the damage multiplier
     * @param damageIncrease the flat damage increase
     */
    public SkillDamageModifier(Skill skill, double damageMultiplier, double damageIncrease) {
        this(skill, damageMultiplier, damageIncrease, 200);
    }
    
    @Override
    public String getName() {
        return skill.getName().toLowerCase().replace(" ", "_");
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
        return new ModifierResult(damageMultiplier, damageIncrease, false, skill.getName());
    }
    
    @Override
    public ModifierType getType() {
        return ModifierType.ABILITY;
    }

    public static class Flat extends SkillDamageModifier {
        public Flat(Skill skill, double damageIncrease, int priority) {
            super(skill, 1.0, damageIncrease, priority);
        }
        public Flat(Skill skill, double damageIncrease) {
            super(skill, 1.0, damageIncrease);
        }
    }

    public static class Multiplier extends SkillDamageModifier {
        public Multiplier(Skill skill, double damageMultiplier, int priority) {
            super(skill, damageMultiplier, 0.0, priority);
        }
        public Multiplier(Skill skill, double damageMultiplier) {
            super(skill, damageMultiplier, 0.0);
        }
    }

}
