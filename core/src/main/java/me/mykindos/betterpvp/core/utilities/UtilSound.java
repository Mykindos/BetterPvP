package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilSound {

    public static void playSound(Entity entity, org.bukkit.Sound sound, float volume, float pitch, boolean followPlayer) {
        playSound(entity, sound, Sound.Source.MASTER, volume, pitch, followPlayer);
    }

    public static void playSound(Entity entity, org.bukkit.Sound sound, Sound.Source source, float volume, float pitch, boolean followPlayer) {
        if (followPlayer) {
            entity.playSound(Sound.sound(sound, source, volume, pitch), Sound.Emitter.self());
        } else {
            entity.playSound(Sound.sound(sound, source, volume, pitch));
        }
    }

    public static void playSound(World world, Location location, org.bukkit.Sound sound, float volume, float pitch) {

        world.playSound(Sound.sound(sound, Sound.Source.MASTER, volume, pitch), location.getX(), location.getY(), location.getZ());
    }

    public static void playSound(World world, Location location, org.bukkit.Sound sound, Sound.Source source, float volume, float pitch) {
        world.playSound(Sound.sound(sound, source, volume, pitch), location.getX(), location.getY(), location.getZ());
    }

    public static void playSound(World world, Entity entity, org.bukkit.Sound sound, float volume, float pitch) {
        world.playSound(Sound.sound(sound, Sound.Source.MASTER, volume, pitch), entity);
    }

    public static void playSound(World world, Entity entity, org.bukkit.Sound sound, Sound.Source source, float volume, float pitch) {
        world.playSound(Sound.sound(sound, source, volume, pitch), entity);
    }
}
