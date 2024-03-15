package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilSound {

    public static void playSound(Player player, org.bukkit.Sound sound, float volume, float pitch, boolean followPlayer) {
        playSound(player, sound, Sound.Source.MASTER, volume, pitch, followPlayer);
    }

    public static void playSound(Player player, org.bukkit.Sound sound, Sound.Source source, float volume, float pitch, boolean followPlayer) {
        if (followPlayer) {
            player.playSound(Sound.sound(sound.key(), source, volume, pitch), Sound.Emitter.self());
        } else {
            player.playSound(Sound.sound(sound.key(), source, volume, pitch));
        }
    }

    public static void playSound(World world, Location location, org.bukkit.Sound sound, float volume, float pitch) {
        world.playSound(Sound.sound(sound.key(), Sound.Source.MASTER, volume, pitch), location.getX(), location.getY(), location.getZ());
    }

    public static void playSound(World world, Location location, org.bukkit.Sound sound, Sound.Source source, float volume, float pitch) {
        world.playSound(Sound.sound(sound.key(), source, volume, pitch), location.getX(), location.getY(), location.getZ());
    }

    public static void playSound(World world, Entity entity, org.bukkit.Sound sound, float volume, float pitch) {
        world.playSound(Sound.sound(sound.key(), Sound.Source.MASTER, volume, pitch), entity);
    }
    public static void playSound(World world, Entity entity, org.bukkit.Sound sound, Sound.Source source, float volume, float pitch) {
        world.playSound(Sound.sound(sound.key(), source, volume, pitch), entity);
    }
}
