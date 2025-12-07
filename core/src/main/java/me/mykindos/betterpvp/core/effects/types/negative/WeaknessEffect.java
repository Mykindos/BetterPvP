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
        return PotionEffectType.UNLUCK;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Weakness " + UtilFormat.getRomanNumeral(level) + " <reset>decreases outgoing melee damage by <stat>" + (level * 20) + "%</stat>";
    }


}
