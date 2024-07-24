package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;

public class ConcussedEffect extends EffectType {

    @Override
    public String getName() {
        return "Concussed";
    }

    @Override
    public boolean isNegative() {
        return true;
    }


    @Override
    public String getDescription(int level) {
        return "<white>Concussion " + UtilFormat.getRomanNumeral(level) + " <reset>decreases attack speed by <val>" + (level * 25) + "</val>%";
    }

}
