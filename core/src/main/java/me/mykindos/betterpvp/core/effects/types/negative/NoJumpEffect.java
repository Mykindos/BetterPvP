package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import org.bukkit.entity.LivingEntity;

public class NoJumpEffect extends EffectType {

    @Override
    public String getName() {
        return "No Jump";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public int defaultAmplifier() {
        return 200;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        //UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(getVanillaPotionType(), effect.getVanillaDuration(), defaultAmplifier()));
    }


}

