package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.EffectType;

public class AttackSpeedEffect extends EffectType {

    @Override
    public String getName() {
        return "Attack Speed";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

}
