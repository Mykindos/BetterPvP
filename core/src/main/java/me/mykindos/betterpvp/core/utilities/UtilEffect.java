package me.mykindos.betterpvp.core.utilities;

import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R3.potion.CraftPotionUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class UtilEffect {

    public static boolean isNegativePotionEffect(PotionEffect effect) {
        return effect.getType().getName().contains("SLOW")
                || effect.getType().getName().contains("CONFUSION")
                || effect.getType().getName().contains("POISON")
                || effect.getType().getName().contains("BLINDNESS")
                || effect.getType().getName().contains("WITHER")
                || effect.getType().getName().contains("LEVITATION")
                || effect.getType().getName().contains("OMEN")
                || effect.getType().getName().contains("DARKNESS");
    }

    public static void applyCraftEffect(LivingEntity livingEntity, PotionEffect effect) {
        CraftLivingEntity craftLivingEntity = (CraftLivingEntity) livingEntity;
        craftLivingEntity.getHandle().addEffect(CraftPotionUtil.fromBukkit(effect), craftLivingEntity.getHandle(), EntityPotionEffectEvent.Cause.PLUGIN, false);
    }

    public static boolean hasPotionEffect(LivingEntity ent, PotionEffectType type, int amplifier) {
        return ent.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getType() == type
                && potionEffect.getAmplifier() >= amplifier);
    }

}
