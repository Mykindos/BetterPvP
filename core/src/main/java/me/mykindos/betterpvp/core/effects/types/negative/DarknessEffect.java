package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.potion.PotionEffectType;

public class DarknessEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Darkness";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.DARKNESS;
    }

}

