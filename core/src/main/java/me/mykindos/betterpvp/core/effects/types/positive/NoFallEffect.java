package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

/**
 * Provides immunity to negative effects
 */
public class NoFallEffect extends EffectType {

    @Override
    public String getName() {
        return "No Fall";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

}
