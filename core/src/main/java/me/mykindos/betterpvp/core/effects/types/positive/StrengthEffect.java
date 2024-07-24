package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class StrengthEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Strength";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.STRENGTH;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Strength " + UtilFormat.getRomanNumeral(level) + " <reset>increases melee damage dealt by <stat>" + (level * 1.5) + "</stat>";
    }

    public String getGenericDescription() {
        return  "<white>" + getName() + "</white> increases melee damage by <val>1.5</val> per level";
    }

}

