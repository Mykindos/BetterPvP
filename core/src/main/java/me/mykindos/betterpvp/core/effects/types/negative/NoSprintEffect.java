package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.EffectType;

public class NoSprintEffect extends EffectType {

    @Override
    public String getName() {
        return "No Sprint";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

}

