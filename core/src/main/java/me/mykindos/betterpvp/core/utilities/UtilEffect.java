package me.mykindos.betterpvp.core.utilities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
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

    /**
     * Determines if the given potion effect is considered a negative effect.
     *
     * @param effect the PotionEffect to evaluate
     * @return true if the potion effect is negative (e.g., SLOW, CONFUSION, POISON, etc.), false otherwise
     */
    public static boolean isNegativePotionEffect(PotionEffect effect) {
        return Stream.of("SLOW", "CONFUSION", "POISON", "BLINDNESS", "WITHER", "LEVITATION", "OMEN", "DARKNESS")
                .anyMatch(s -> effect.getType().getKey().getKey().toUpperCase().contains(s));
    }

    /**
     * Applies a custom potion effect to the specified {@link LivingEntity}.
     * This method utilizes CraftBukkit internal methods to apply the effect.
     *
     * @param livingEntity the living entity to apply the potion effect to
     * @param effect the potion effect to be applied
     */
    public static void applyCraftEffect(LivingEntity livingEntity, PotionEffect effect) {
        CraftLivingEntity craftLivingEntity = (CraftLivingEntity) livingEntity;

        craftLivingEntity.getHandle().addEffect(CraftPotionUtil.fromBukkit(effect), craftLivingEntity.getHandle(), EntityPotionEffectEvent.Cause.PLUGIN, false);
    }

    /**
     * Checks if the given entity has a specific potion effect with at least the specified amplifier level.
     *
     * @param ent the living entity to check for active potion effects
     * @param type the type of the potion effect to check
     * @param amplifier the minimum amplifier level of the potion effect to check
     * @return true if the entity has the specified potion effect with the required amplifier level or higher, false otherwise
     */
    public static boolean hasPotionEffect(LivingEntity ent, PotionEffectType type, int amplifier) {
        return ent.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getType() == type
                && potionEffect.getAmplifier() >= amplifier);
    }

    public static void sendPacketPotionEffect(LivingEntity ent, PotionEffectType type, int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon) {

        byte flags = 0;
        if(ambient) flags |= 0x1;
        if(showParticles) flags |= 0x2;
        if(showIcon) flags |= 0x4;

        WrapperPlayServerEntityEffect potionPacket = new WrapperPlayServerEntityEffect(ent.getEntityId(), PotionTypes.getByName(type.key().asMinimalString()), amplifier, duration, flags);
        PacketEvents.getAPI().getPlayerManager().sendPacket(ent, potionPacket);
    }

    public static void sendPacketPotionEffectRemove(LivingEntity ent, PotionEffectType type) {
        WrapperPlayServerRemoveEntityEffect potionPacket = new WrapperPlayServerRemoveEntityEffect(ent.getEntityId(), PotionTypes.getByName(type.key().asMinimalString()));
        PacketEvents.getAPI().getPlayerManager().sendPacket(ent, potionPacket);
    }

}
