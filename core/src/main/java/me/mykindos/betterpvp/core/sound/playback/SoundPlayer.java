package me.mykindos.betterpvp.core.sound.playback;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.sound.model.SoundCategory;
import me.mykindos.betterpvp.core.sound.model.SoundDefinition;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Utility class for playing sounds to players
 * Handles player preferences, location-based playback, and main thread synchronization
 */
public class SoundPlayer {

    private final BPvPPlugin plugin;
    private final ClientManager clientManager;

    public SoundPlayer(BPvPPlugin plugin, ClientManager clientManager) {
        this.plugin = plugin;
        this.clientManager = clientManager;
    }

    /**
     * Plays a sound to multiple players
     *
     * @param playerIds The player UUIDs to play the sound to
     * @param soundKey The sound key (namespace:key)
     * @param volume The volume
     * @param pitch The pitch
     * @param category The sound category for preference checking
     * @param location Optional location for location-based playback
     */
    public void playSound(Collection<UUID> playerIds, String soundKey, float volume, float pitch,
                         SoundCategory category, Location location) {
        runOnMainThread(() -> {
            //noinspection PatternValidation
            Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch);

            for (UUID playerId : playerIds) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    if (!shouldPlaySound(player, category)) {
                        continue;
                    }

                    if (location != null) {
                        player.playSound(sound, location.getX(), location.getY(), location.getZ());
                    } else {
                        player.playSound(sound);
                    }
                }
            }
        });
    }

    /**
     * Plays a sound definition to multiple players
     */
    public void playSound(Collection<UUID> playerIds, SoundDefinition soundDef,
                         SoundCategory category, Location location) {
        runOnMainThread(() -> {
            Sound sound = soundDef.toAdventureSound();

            for (UUID playerId : playerIds) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    if (!shouldPlaySound(player, category)) {
                        continue;
                    }

                    if (location != null) {
                        player.playSound(sound, location.getX(), location.getY(), location.getZ());
                    } else {
                        player.playSound(sound);
                    }
                }
            }
        });
    }

    /**
     * Stops a sound for a player
     */
    public void stopSound(Player player, String soundKey) {
        runOnMainThread(() -> {
            //noinspection PatternValidation
            player.stopSound(Sound.sound(Key.key(soundKey), Sound.Source.MASTER, 1.0f, 1.0f));
        });
    }

    /**
     * Checks if a sound should be played for a player based on their preferences
     */
    private boolean shouldPlaySound(Player player, SoundCategory category) {
        Client client = clientManager.search().online(player);

        ClientProperty property = switch (category) {
            case MUSIC -> ClientProperty.MUSIC_ENABLED;
            case AMBIENT -> ClientProperty.AMBIENT_ENABLED;
            case UI -> ClientProperty.UI_SOUNDS_ENABLED;
        };

        return (boolean) client.getProperty(property).orElse(true);
    }

    /**
     * Runs a task on the Bukkit main thread
     */
    private void runOnMainThread(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

}
