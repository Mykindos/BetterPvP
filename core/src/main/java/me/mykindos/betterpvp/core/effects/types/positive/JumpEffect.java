package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

public class JumpEffect extends EffectType {

    @Override
    public String getName() {
        return "Jump";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

}

