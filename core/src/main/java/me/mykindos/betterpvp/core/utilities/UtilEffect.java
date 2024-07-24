package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilEffect {

    public static boolean isNegativePotionEffect(PotionEffect effect) {
        return Stream.of("SLOW", "CONFUSION", "POISON", "BLINDNESS", "WITHER", "LEVITATION", "OMEN", "DARKNESS")
                .anyMatch(s -> effect.getType().getKey().getKey().toUpperCase().contains(s));
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
