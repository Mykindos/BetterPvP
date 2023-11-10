package me.mykindos.betterpvp.core.utilities.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundEffect {

    public static final SoundEffect HIGH_PITCH_PLING = new SoundEffect(Sound.BLOCK_NOTE_BLOCK_PLING, 1.6F);
    public static final SoundEffect LOW_PITCH_PLING = new SoundEffect(Sound.BLOCK_NOTE_BLOCK_PLING, 0.6F);
    public static final SoundEffect WRONG_ACTION = new SoundEffect(Sound.ENTITY_ITEM_BREAK, 0.6F);

    private final Sound sound;
    private final float pitch;
    private final float volume;

    public SoundEffect(final Sound sound, final float pitch, final float volume) {
        this.sound = sound;
        this.pitch = pitch;
        this.volume = volume;
    }

    public SoundEffect(final Sound sound, final float pitch) {
        this(sound, pitch, 1F);
    }

    public SoundEffect(final Sound sound) {
        this(sound, 1F, 1F);
    }

    /**
     * Plays the sound effect to all players at a location
     *
     * @param location The location to play the sound effect at
     */
    public void play(final Location location) {
        location.getWorld().playSound(location, this.sound, this.volume, this.pitch);
    }

    /**
     * Plays a sound effect to a player at their location
     *
     * @param player The player to play the sound effect to
     */
    public void play(final Player player) {
        player.playSound(player.getLocation(), this.sound, this.volume, this.pitch);
    }

    /**
     * Plays a sound effect to a player at a location
     *
     * @param player   The player to play the sound effect to
     * @param location The location to play the sound effect at
     */
    public void play(final Player player, final Location location) {
        player.playSound(location, this.sound, this.volume, this.pitch);
    }

}
