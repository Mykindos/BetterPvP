package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class WeaknessEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Weakness";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.OOZING;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Weakness " + UtilFormat.getRomanNumeral(level) + " <reset>decreases melee damage dealt by <stat>" + (level * 1.0) + "</stat>";
    }

    @Override
    public String getGenericDescription() {
        return  "<white>" + getName() + "</white> decreases melee damage by <green>1.0</green> per level";
    }
}
