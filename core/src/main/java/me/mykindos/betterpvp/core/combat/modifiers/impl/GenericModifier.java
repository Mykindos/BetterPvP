package me.mykindos.betterpvp.core.combat.modifiers.impl;


import lombok.AllArgsConstructor;
import lombok.With;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
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
    private final double multiplier;
    private final double flat;

    public GenericModifier(String reason, double multiplier, double flat) {
        this.reason = reason;
        this.multiplier = multiplier;
        this.flat = flat;
    }

    @Override
    public String getName() {
        return reason;
    }
    
    @Override
    public int getPriority() {
        return 150; // Medium priority
    }
    
    @Override
    public boolean canApply(DamageEvent event) {
        return true;
    }
    
    @Override
    public ModifierResult apply(DamageEvent event) {
        return new ModifierResult(multiplier, flat, false, reason);
    }
    
    @Override
    public ModifierType getType() {
        return type;
    }

}
