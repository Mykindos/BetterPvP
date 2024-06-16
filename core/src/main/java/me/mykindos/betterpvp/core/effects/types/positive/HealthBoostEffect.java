package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + " " + UtilFormat.getRomanNumeral(level) + "</white> increases health by <stat>" + level * 20 + "</stat>%";
    }
}
