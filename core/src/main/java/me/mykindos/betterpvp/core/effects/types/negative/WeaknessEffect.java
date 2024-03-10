package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
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
        return PotionEffectType.WEAKNESS;
    }

}
