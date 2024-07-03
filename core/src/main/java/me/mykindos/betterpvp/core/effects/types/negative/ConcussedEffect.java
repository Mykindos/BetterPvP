package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class ConcussedEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Concussed";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.SLOW_DIGGING;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Concussion " + UtilFormat.getRomanNumeral(level) + " <reset>decreases attack speed by <val>" + (level * 25) + "</val>%";
    }

}
