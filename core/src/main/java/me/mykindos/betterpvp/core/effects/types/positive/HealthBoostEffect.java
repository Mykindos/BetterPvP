package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.potion.PotionEffectType;

/**
 * Provides immunity to negative effects
 */
public class HealthBoostEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Health Boost";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.HEALTH_BOOST;
    }
}
