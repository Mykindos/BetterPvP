package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class PoisonEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Poison";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.POISON;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Poison " + UtilFormat.getRomanNumeral(level) + "</white> deals <val>" + level + "</val> damage every <stat>" + (25d/20d) + "</stat> seconds";
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white>" + " deals <green>1</green> damage per Level every <yellow>" + (25d/20d) + "</yellow> seconds";
    }
}

