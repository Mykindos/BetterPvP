package me.mykindos.betterpvp.core.effects.types.negative;

import jdk.jshell.execution.Util;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class ConcussedEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Concussed";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.SLOW_DIGGING;
    }
}
