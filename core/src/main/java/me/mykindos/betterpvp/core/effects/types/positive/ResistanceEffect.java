package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.potion.PotionEffectType;

public class ResistanceEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Resistance";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.DAMAGE_RESISTANCE;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Resistance " + UtilFormat.getRomanNumeral(level) + " <reset>decreases incoming damage by <stat>" + (level * 20) + "%</stat>";
    }

}

