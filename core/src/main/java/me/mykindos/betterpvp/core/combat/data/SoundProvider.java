package me.mykindos.betterpvp.core.combat.data;

import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Provides a sound for a given custom damage event.
 */
public interface SoundProvider extends Function<@NotNull CustomDamageEvent, @Nullable Sound> {

    /**
     * The default sound provider. Uses the default Minecraft sounds.
     */
    SoundProvider DEFAULT = event -> {
        final LivingEntity damagee = event.getDamagee();
        final org.bukkit.Sound sound = damagee.getHurtSound();
        if (sound == null) {
            return null;
        }

        return Sound.sound(sound.key(), getSource(damagee), 1f, 1f);
    };

    /**
     * No sounds will be played.
     */
    SoundProvider NONE = event -> null;

    static @NotNull Sound.Source getSource(@Nullable LivingEntity entity) {
        Sound.Source source = entity instanceof Enemy ? Sound.Source.HOSTILE : Sound.Source.NEUTRAL;
        if (entity instanceof Player) {
            source = Sound.Source.PLAYER;
        }

        return source;
    }

    /**
     * @return Whether this sound provider should be emitted from the entity
     */
    default boolean fromEntity() {
        return true;
    }

}
