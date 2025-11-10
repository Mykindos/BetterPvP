package me.mykindos.betterpvp.core.combat.modifiers.impl;


import lombok.AllArgsConstructor;
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
    private final DamageOperator operator;
    private final double operand;

    public GenericModifier(String reason, DamageOperator operator, double operand) {
        this.reason = reason;
        this.operator = operator;
        this.operand = operand;
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
        return new ModifierResult(operand, operator, reason);
    }
    
    @Override
    public ModifierType getType() {
        return type;
    }

}
