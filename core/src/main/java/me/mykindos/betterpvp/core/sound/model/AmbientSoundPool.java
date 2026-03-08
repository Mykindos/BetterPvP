package me.mykindos.betterpvp.core.sound.model;

import lombok.Getter;
import me.mykindos.betterpvp.core.sound.SoundManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a pool of ambient sounds that are randomly played at intervals
 * At each interval, one sound is randomly selected from the pool and played
 */
@Getter
public class AmbientSoundPool {

    private final List<SoundDefinition> sounds;
    private final int minInterval;
    private final int maxInterval;
    private final SoundManager soundManager;

    private AmbientSoundPool(Builder builder) {
        this.sounds = new ArrayList<>(builder.sounds);
        this.minInterval = builder.minInterval;
        this.maxInterval = builder.maxInterval;
        this.soundManager = builder.soundManager;
    }

    /**
     * Starts the ambient sound pool for the specified players
     *
     * @param players The players to play the ambient sounds for
     * @return The AmbientSoundInstance handle for controlling playback
     */
    public AmbientSoundInstance start(Player... players) {
        return start(null, players);
    }

    /**
     * Starts the ambient sound pool at a specific location
     *
     * @param location The location to play the sounds at
     * @param players The players who can hear the sounds
     * @return The AmbientSoundInstance handle for controlling playback
     */
    public AmbientSoundInstance start(Location location, Player... players) {
        Set<Player> playerSet = new HashSet<>(Arrays.asList(players));
        AmbientSoundInstance instance = new AmbientSoundInstance(this, playerSet, location);

        if (soundManager != null) {
            soundManager.registerAmbientInstance(instance);
        }

        return instance;
    }

    /**
     * Gets a random sound from the pool
     *
     * @return A randomly selected sound definition
     */
    public SoundDefinition getRandomSound() {
        if (sounds.isEmpty()) {
            return null;
        }
        return sounds.get((int) (Math.random() * sounds.size()));
    }

    /**
     * Creates a new builder for constructing an AmbientSoundPool
     *
     * @return A new AmbientSoundPool builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating AmbientSoundPool instances
     */
    public static class Builder {
        private final List<SoundDefinition> sounds = new ArrayList<>();
        private int minInterval = 20; // Default: 20 seconds
        private int maxInterval = 60; // Default: 60 seconds
        private SoundManager soundManager;

        /**
         * Adds a sound to the pool
         *
         * @param soundKey The sound key in format "namespace:key"
         * @param volume The volume (0.0 to 1.0)
         * @param pitch The pitch (0.5 to 2.0)
         * @return This builder
         */
        public Builder addSound(String soundKey, float volume, float pitch) {
            this.sounds.add(new SoundDefinition(soundKey, volume, pitch));
            return this;
        }

        /**
         * Adds a sound to the pool with default volume and pitch
         *
         * @param soundKey The sound key in format "namespace:key"
         * @return This builder
         */
        public Builder addSound(String soundKey) {
            this.sounds.add(new SoundDefinition(soundKey));
            return this;
        }

        /**
         * Sets the interval range for playing sounds
         *
         * @param minSeconds Minimum interval in seconds
         * @param maxSeconds Maximum interval in seconds
         * @return This builder
         */
        public Builder interval(int minSeconds, int maxSeconds) {
            this.minInterval = minSeconds;
            this.maxInterval = maxSeconds;
            return this;
        }

        /**
         * Sets the sound manager (internal use)
         *
         * @param soundManager The sound manager
         * @return This builder
         */
        public Builder soundManager(SoundManager soundManager) {
            this.soundManager = soundManager;
            return this;
        }

        /**
         * Builds the AmbientSoundPool
         *
         * @return The constructed AmbientSoundPool
         */
        public AmbientSoundPool build() {
            if (sounds.isEmpty()) {
                throw new IllegalStateException("At least one sound must be added to the pool");
            }
            if (minInterval <= 0 || maxInterval <= 0 || minInterval > maxInterval) {
                throw new IllegalStateException("Invalid interval range");
            }
            return new AmbientSoundPool(this);
        }
    }

}
