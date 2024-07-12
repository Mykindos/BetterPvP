package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.potion.PotionEffectType;

public class SlownessEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Slowness";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.SLOWNESS;
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> reduces player movement by <val>" + 15 * level + "</val>% and prevents the use of movement abilities";
    }
}

