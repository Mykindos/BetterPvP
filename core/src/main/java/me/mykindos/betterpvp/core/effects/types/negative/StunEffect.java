package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.EffectType;

public class StunEffect extends EffectType {

    @Override
    public String getName() {
        return "Stun";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

}
