package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.potion.PotionEffectType;

public class JumpEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Jump";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.JUMP;
    }

}

