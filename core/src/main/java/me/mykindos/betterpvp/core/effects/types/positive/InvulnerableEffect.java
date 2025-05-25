package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

/**
 * Provides immunity to damage
 */
public class InvulnerableEffect extends EffectType {

    @Override
    public String getName() {
        return "Invulnerable";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
