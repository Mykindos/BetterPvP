package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

public class EnergyReductionEffect extends EffectType {

    @Override
    public String getName() {
        return "Energy Reduction";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

}
