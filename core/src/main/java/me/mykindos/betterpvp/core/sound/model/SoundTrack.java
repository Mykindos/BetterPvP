package me.mykindos.betterpvp.core.sound.model;

import lombok.Getter;
import me.mykindos.betterpvp.core.sound.SoundManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a music track that can be:
 * - Three-part: intro -> loop -> finale
 * - Looping: single sound that repeats
 * - One-shot: single sound that plays once
 * All segments share the same volume and pitch
 */
@Getter
public class SoundTrack {

    private final String introSound;
    private final String loopSound;
    private final String finaleSound;
    private final int introDuration; // seconds
    private final int loopDuration; // seconds
    private final int finaleDuration; // seconds
    private final boolean shouldLoop; // if false, plays once and stops
    private final float volume;
    private final float pitch;
    private final SoundManager soundManager;

    private SoundTrack(Builder builder) {
        this.introSound = builder.introSound;
        this.loopSound = builder.loopSound;
        this.finaleSound = builder.finaleSound;
        this.introDuration = builder.introDuration;
        this.loopDuration = builder.loopDuration;
        this.finaleDuration = builder.finaleDuration;
        this.shouldLoop = builder.shouldLoop;
        this.volume = builder.volume;
        this.pitch = builder.pitch;
        this.soundManager = builder.soundManager;
    }

    /**
     * Plays the sound track for the specified players
     *
     * @param players The players to play the track for
     * @return The SoundTrackInstance handle for controlling playback
     */
    public SoundTrackInstance play(Player... players) {
        return play(null, players);
    }

    /**
     * Plays the sound track at a specific location
     *
     * @param location The location to play the track at
     * @param players The players who can hear the track
     * @return The SoundTrackInstance handle for controlling playback
     */
    public SoundTrackInstance play(Location location, Player... players) {
        Set<Player> playerSet = new HashSet<>(Arrays.asList(players));
        SoundTrackInstance instance = new SoundTrackInstance(this, playerSet, location);

        if (soundManager != null) {
            soundManager.registerTrackInstance(instance);
        }

        return instance;
    }

    /**
     * Creates a new builder for constructing a SoundTrack
     *
     * @return A new SoundTrack builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating SoundTrack instances
     */
    public static class Builder {
        private String introSound;
        private String loopSound;
        private String finaleSound;
        private int introDuration = 5; // default 5 seconds
        private int loopDuration = 30; // default 30 seconds
        private int finaleDuration = 10; // default 10 seconds
        private boolean shouldLoop = true; // default: loop forever
        private float volume = 1.0f;
        private float pitch = 1.0f;
        private SoundManager soundManager;

        /**
         * Sets the intro sound (plays once at the beginning)
         *
         * @param soundKey The sound key in format "namespace:key"
         * @param durationSeconds The duration of the intro in seconds
         * @return This builder
         */
        public Builder introSound(String soundKey, int durationSeconds) {
            this.introSound = soundKey;
            this.introDuration = durationSeconds;
            return this;
        }

        /**
         * Sets the intro sound with default duration
         *
         * @param soundKey The sound key in format "namespace:key"
         * @return This builder
         */
        public Builder introSound(String soundKey) {
            this.introSound = soundKey;
            return this;
        }

        /**
         * Sets the loop sound (plays repeatedly until finale is triggered)
         *
         * @param soundKey The sound key in format "namespace:key"
         * @param durationSeconds The duration of the loop in seconds
         * @return This builder
         */
        public Builder loopSound(String soundKey, int durationSeconds) {
            this.loopSound = soundKey;
            this.loopDuration = durationSeconds;
            return this;
        }

        /**
         * Sets the loop sound with default duration
         *
         * @param soundKey The sound key in format "namespace:key"
         * @return This builder
         */
        public Builder loopSound(String soundKey) {
            this.loopSound = soundKey;
            return this;
        }

        /**
         * Sets the finale sound (plays once when finale is triggered)
         *
         * @param soundKey The sound key in format "namespace:key"
         * @param durationSeconds The duration of the finale in seconds
         * @return This builder
         */
        public Builder finaleSound(String soundKey, int durationSeconds) {
            this.finaleSound = soundKey;
            this.finaleDuration = durationSeconds;
            return this;
        }

        /**
         * Sets the finale sound with default duration
         *
         * @param soundKey The sound key in format "namespace:key"
         * @return This builder
         */
        public Builder finaleSound(String soundKey) {
            this.finaleSound = soundKey;
            return this;
        }

        /**
         * Sets a single sound that will loop continuously
         *
         * @param soundKey The sound key in format "namespace:key"
         * @param durationSeconds The duration of the sound in seconds
         * @return This builder
         */
        public Builder singleLoopingSound(String soundKey, int durationSeconds) {
            this.loopSound = soundKey;
            this.loopDuration = durationSeconds;
            this.shouldLoop = true;
            return this;
        }

        /**
         * Sets a single sound that will play once and stop
         *
         * @param soundKey The sound key in format "namespace:key"
         * @param durationSeconds The duration of the sound in seconds
         * @return This builder
         */
        public Builder singleSound(String soundKey, int durationSeconds) {
            this.loopSound = soundKey;
            this.loopDuration = durationSeconds;
            this.shouldLoop = false;
            return this;
        }

        /**
         * Sets whether the track should loop continuously
         * Only applies to the loop segment
         *
         * @param shouldLoop true to loop, false to play once
         * @return This builder
         */
        public Builder loop(boolean shouldLoop) {
            this.shouldLoop = shouldLoop;
            return this;
        }

        /**
         * Sets the volume for all segments
         *
         * @param volume The volume (0.0 to 1.0)
         * @return This builder
         */
        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Sets the pitch for all segments
         *
         * @param pitch The pitch (0.5 to 2.0)
         * @return This builder
         */
        public Builder pitch(float pitch) {
            this.pitch = pitch;
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
         * Builds the SoundTrack
         *
         * @return The constructed SoundTrack
         */
        public SoundTrack build() {
            if (loopSound == null) {
                throw new IllegalStateException("Loop sound is required");
            }
            return new SoundTrack(this);
        }
    }

}
