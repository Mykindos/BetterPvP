package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class SpeedEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Speed";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.SPEED;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Speed " + UtilFormat.getRomanNumeral(level) + " <reset>increases movement speed by <val>" + (level * 20) + "</val>%";
    }

    public String getGenericDescription() {
        return  "<white>" + getName() + "</white> increases movement speed by <green>20</green>% per level";
    }

}

