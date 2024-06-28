package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.potion.PotionEffectType;

/**
 * Provides immunity to negative effects
 */
public class FireResistanceEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Fire Resistance";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.FIRE_RESISTANCE;
    }

}
