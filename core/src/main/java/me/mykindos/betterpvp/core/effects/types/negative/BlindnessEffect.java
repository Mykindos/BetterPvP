package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.potion.PotionEffectType;

public class BlindnessEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Blindness";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.BLINDNESS;
    }

    @Override
    public String getDescription(int level) {
        return "<white>Blindness</white> reduces a players vision and prevents them from beginning a sprint";
    }
}

