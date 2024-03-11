package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

public class CooldownReductionEffect extends EffectType {

    @Override
    public String getName() {
        return "Cooldown Reduction";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

}
