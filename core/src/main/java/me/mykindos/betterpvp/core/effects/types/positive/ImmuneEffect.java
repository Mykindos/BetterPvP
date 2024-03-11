package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

/**
 * Provides immunity to negative effects
 */
public class ImmuneEffect extends EffectType {

    @Override
    public String getName() {
        return "Immune";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

}
