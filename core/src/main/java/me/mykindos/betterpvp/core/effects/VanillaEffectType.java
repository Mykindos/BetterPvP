package me.mykindos.betterpvp.core.effects;

import me.mykindos.betterpvp.core.utilities.UtilEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class VanillaEffectType extends EffectType {


    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(getVanillaPotionType(), effect.getVanillaDuration(), effect.getAmplifier() - 1));
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect) {
        livingEntity.removePotionEffect(getVanillaPotionType());
    }

    public void checkActive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(getVanillaPotionType(), effect.getRemainingVanillaDuration(), effect.getAmplifier() - 1));
    }

    public abstract PotionEffectType getVanillaPotionType();

}
