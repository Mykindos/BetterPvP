package me.mykindos.betterpvp.core.utilities.model;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
public class SoundEffect {

    public static final SoundEffect HIGH_PITCH_PLING = new SoundEffect(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.6F);
    public static final SoundEffect LOW_PITCH_PLING = new SoundEffect(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.6F);
    public static final SoundEffect WRONG_ACTION = new SoundEffect(org.bukkit.Sound.ENTITY_ITEM_BREAK, 0.6F);

    private final Sound sound;

    public SoundEffect(final org.bukkit.Sound sound, final float pitch, final float volume) {
        this(Sound.sound(sound.key(), Sound.Source.MASTER, volume, pitch));
    }

    public SoundEffect(final org.bukkit.Sound sound, final float pitch) {
        this(sound, pitch, 1F);
    }

    public SoundEffect(final org.bukkit.Sound sound) {
        this(sound, 1F, 1F);
    }

    public SoundEffect(final String namespace, final String key, final float pitch, final float volume) {
        //noinspection PatternValidation
        this.sound = Sound.sound(Key.key(namespace, key), net.kyori.adventure.sound.Sound.Source.MASTER, volume, pitch);
    }

    public SoundEffect(final String namespace, final String key, final float pitch) {
        this(namespace, key, pitch, 1F);
    }

    public SoundEffect(final String namespace, final String key) {
        this(namespace, key, 1F, 1F);
    }

    public SoundEffect(final Sound sound) {
        this.sound = sound;
    }

    /**
     * Plays the sound effect to all players at a location
     *
     * @param location The location to play the sound effect at
     */
    public void play(final Location location) {
        location.getWorld().playSound(location, this.sound.name().asString(), this.sound.volume(), this.sound.pitch());
    }

    /**
     * Plays a sound effect to a player at their location
     *
     * @param player The player to play the sound effect to
     */
    public void play(final Player player) {
        player.playSound(this.sound);
    }

    /**
     * Plays a sound effect to a player at a location
     *
     * @param player   The player to play the sound effect to
     * @param location The location to play the sound effect at
     */
    public void play(final Player player, final Location location) {
        player.playSound(location, this.sound.name().asString(), this.sound.volume(), this.sound.pitch());
    }

}
