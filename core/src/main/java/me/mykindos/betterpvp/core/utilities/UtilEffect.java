package me.mykindos.betterpvp.core.utilities;

import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    /**
     * Check to see if a {@link LivingEntity} has the specified {@link PotionEffectType} with the correct {@link PotionEffect#getAmplifier()} and {@link PotionEffect#getDuration()}
     * @param ent the target {@link LivingEntity}
     * @param type the {@link PotionEffectType}
     * @param duration the {@link PotionEffect#getDuration()}
     * @param amplifier the {@link PotionEffect#getAmplifier()}
     * @return {@code true} if the target {@link LivingEntity} has a {@link PotionEffect} with the following:
     * <p>{@link PotionEffect#getAmplifier()} {@code >} the supplied {@code amplifier}</p>
     * <p>{@link PotionEffect#getAmplifier()} {@code =} the supplied {@code amplifier} and {@link PotionEffect#getDuration()} {@code >=} the supplied {@code duration}</p>
     * {@code false} otherwise
     */
    public static boolean hasPotionEffect(LivingEntity ent, PotionEffectType type, int duration, int amplifier) {
        //return true if the living entity already has an effect with a higher amplifier
        if (ent.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getType() == type
                && potionEffect.getAmplifier() > amplifier)) return true;
        //else, return true if the living entity if there exists a potion effect with an equal or greater than length of the same amplifier
        return ent.getActivePotionEffects().stream()
                .anyMatch(potionEffect -> potionEffect.getType() == type &&
                                potionEffect.getAmplifier() == amplifier &&
                                (potionEffect.getDuration() >= duration || potionEffect.getDuration() == -1));

    }

}
