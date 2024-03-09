package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NoJumpEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "No Jump";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.JUMP;
    }

    @Override
    public int defaultAmplifier() {
        return 200;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(getVanillaPotionType(), effect.getVanillaDuration() + 1, defaultAmplifier()));
    }

    @Override
    public void checkActive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(getVanillaPotionType(), effect.getRemainingVanillaDuration() + 1, defaultAmplifier()));
    }

}

