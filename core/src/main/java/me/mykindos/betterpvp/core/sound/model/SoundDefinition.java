package me.mykindos.betterpvp.core.sound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Represents a sound definition with a namespace:key identifier, volume, and pitch
 */
@Getter
@AllArgsConstructor
public class SoundDefinition {

    private final String soundKey;
    private final float volume;
    private final float pitch;

    /**
     * Creates a sound definition with default volume (1.0) and pitch (1.0)
     *
     * @param soundKey The sound key in format "namespace:key"
     */
    public SoundDefinition(String soundKey) {
        this(soundKey, 1.0f, 1.0f);
    }

    /**
     * Creates an Adventure Sound object from this definition
     *
     * @return The Adventure Sound object
     */
    public Sound toAdventureSound() {
        //noinspection PatternValidation
        return Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch);
    }

}
