package me.mykindos.betterpvp.core.combat.modifiers.impl;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;

/**
 * Modifier that applies environmental damage effects based on location, weather, etc.
 */
@AllArgsConstructor
public class GenericModifier implements DamageModifier {

    private final String reason;
    @With
    private ModifierType type = ModifierType.GENERIC;
    @Getter
    private final DamageOperator damageOperator;
    @Getter
    private final double damageOperand;

    public GenericModifier(String reason, DamageOperator damageOperator, double damageOperand) {
        this.reason = reason;
        this.damageOperator = damageOperator;
        this.damageOperand = damageOperand;
    }

    @Override
    public String getName() {
        return reason;
    }

    @Override
    public int getPriority() {
        return 500;
    }
    
    @Override
    public boolean canApply(DamageEvent event) {
        return true;
    }
    
    @Override
    public ModifierResult apply(DamageEvent event) {
        return new ModifierResult(damageOperand, damageOperator, reason);
    }
    
    @Override
    public ModifierType getType() {
        return type;
    }

}
