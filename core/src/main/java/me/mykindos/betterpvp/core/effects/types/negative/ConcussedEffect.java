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
        return "Concuss";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.SLOW_DIGGING;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        super.onReceive(livingEntity, effect);
        UtilMessage.simpleMessage(livingEntity, "Have fun - CTE");
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect) {
        super.onExpire(livingEntity, effect);
        UtilMessage.simpleMessage(livingEntity, "Bai Bai");
    }
}
