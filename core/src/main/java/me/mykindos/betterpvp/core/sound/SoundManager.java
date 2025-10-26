package me.mykindos.betterpvp.core.sound;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.sound.model.AmbientSoundInstance;
import me.mykindos.betterpvp.core.sound.model.AmbientSoundPool;
import me.mykindos.betterpvp.core.sound.model.SoundCategory;
import me.mykindos.betterpvp.core.sound.model.SoundDefinition;
import me.mykindos.betterpvp.core.sound.model.SoundTrack;
import me.mykindos.betterpvp.core.sound.model.SoundTrackInstance;
import me.mykindos.betterpvp.core.sound.playback.AmbientPlaybackHandler;
import me.mykindos.betterpvp.core.sound.playback.SoundPlayer;
import me.mykindos.betterpvp.core.sound.playback.TrackPlaybackHandler;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central manager for the sound system
 * Delegates playback logic to specialized handlers
 * Manages instance tracking, lifecycle, and cleanup
 */
@Singleton
@CustomLog
public class SoundManager {

    private final ScheduledExecutorService executorService;

    // Playback components
    private final SoundPlayer soundPlayer;
    private final TrackPlaybackHandler trackHandler;
    private final AmbientPlaybackHandler ambientHandler;

    // Instance tracking
    private final Map<UUID, List<SoundTrackInstance>> playerTracks;
    private final Map<UUID, List<AmbientSoundInstance>> playerAmbient;

    @Inject
    public SoundManager(Core plugin, ClientManager clientManager) {
        // Initialize thread executor
        this.executorService = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "BPvP-SoundManager");
            thread.setDaemon(true);
            return thread;
        });

        // Initialize playback components
        this.soundPlayer = new SoundPlayer(plugin, clientManager);
        this.trackHandler = new TrackPlaybackHandler(soundPlayer, executorService, this::cleanupTrackInstance);
        this.ambientHandler = new AmbientPlaybackHandler(soundPlayer, executorService);

        // Initialize tracking maps
        this.playerTracks = new ConcurrentHashMap<>();
        this.playerAmbient = new ConcurrentHashMap<>();
    }

    /**
     * Registers and starts playback of a sound track
     */
    public void registerTrackInstance(SoundTrackInstance instance) {
        // Add to tracking
        instance.getPlayerIds().forEach(playerId -> {
            playerTracks.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>()).add(instance);
        });

        // Start playback via handler
        trackHandler.startPlayback(instance);
    }

    /**
     * Registers and starts playback of an ambient sound pool
     */
    public void registerAmbientInstance(AmbientSoundInstance instance) {
        // Add to tracking
        instance.getPlayerIds().forEach(playerId -> {
            playerAmbient.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>()).add(instance);
        });

        // Start playback via handler
        ambientHandler.startPlayback(instance);
    }

    /**
     * Stops all sounds for a player in a specific category
     * Called when player disables a sound category
     */
    public void stopSoundsForPlayer(Player player, SoundCategory category) {
        if (category == SoundCategory.MUSIC) {
            stopMusicForPlayer(player);
        } else if (category == SoundCategory.AMBIENT) {
            stopAmbientForPlayer(player);
        }
    }

    /**
     * Stops all music tracks for a player
     */
    private void stopMusicForPlayer(Player player) {
        List<SoundTrackInstance> tracks = playerTracks.get(player.getUniqueId());
        if (tracks != null) {
            for (SoundTrackInstance track : tracks) {
                SoundTrack soundTrack = track.getTrack();
                if (soundTrack.getIntroSound() != null) {
                    soundPlayer.stopSound(player, soundTrack.getIntroSound());
                }
                if (soundTrack.getLoopSound() != null) {
                    soundPlayer.stopSound(player, soundTrack.getLoopSound());
                }
                if (soundTrack.getFinaleSound() != null) {
                    soundPlayer.stopSound(player, soundTrack.getFinaleSound());
                }
            }
        }
    }

    /**
     * Stops all ambient sounds for a player
     */
    private void stopAmbientForPlayer(Player player) {
        List<AmbientSoundInstance> ambient = playerAmbient.get(player.getUniqueId());
        if (ambient != null) {
            for (AmbientSoundInstance instance : ambient) {
                for (SoundDefinition sound : instance.getPool().getSounds()) {
                    soundPlayer.stopSound(player, sound.getSoundKey());
                }
            }
        }
    }

    /**
     * Cleans up all sound instances for a player (called on disconnect)
     * Only stops instances if no other players remain
     */
    public void cleanupPlayer(UUID playerId) {
        cleanupPlayerTracks(playerId);
        cleanupPlayerAmbient(playerId);
    }

    /**
     * Cleans up track instances for a player
     */
    private void cleanupPlayerTracks(UUID playerId) {
        List<SoundTrackInstance> tracks = playerTracks.remove(playerId);
        if (tracks != null) {
            for (SoundTrackInstance track : tracks) {
                boolean hasPlayersRemaining = track.removePlayer(playerId);
                if (!hasPlayersRemaining) {
                    track.stop();
                    cleanupTrackInstance(track);
                }
            }
        }
    }

    /**
     * Cleans up ambient instances for a player
     */
    private void cleanupPlayerAmbient(UUID playerId) {
        List<AmbientSoundInstance> ambient = playerAmbient.remove(playerId);
        if (ambient != null) {
            for (AmbientSoundInstance instance : ambient) {
                boolean hasPlayersRemaining = instance.removePlayer(playerId);
                if (!hasPlayersRemaining) {
                    instance.stop();
                    cleanupAmbientInstance(instance);
                }
            }
        }
    }

    /**
     * Removes a track instance from all player tracking
     */
    private void cleanupTrackInstance(SoundTrackInstance instance) {
        instance.getPlayerIds().forEach(playerId -> {
            List<SoundTrackInstance> tracks = playerTracks.get(playerId);
            if (tracks != null) {
                tracks.remove(instance);
                if (tracks.isEmpty()) {
                    playerTracks.remove(playerId);
                }
            }
        });
    }

    /**
     * Removes an ambient instance from all player tracking
     */
    private void cleanupAmbientInstance(AmbientSoundInstance instance) {
        instance.getPlayerIds().forEach(playerId -> {
            List<AmbientSoundInstance> ambient = playerAmbient.get(playerId);
            if (ambient != null) {
                ambient.remove(instance);
                if (ambient.isEmpty()) {
                    playerAmbient.remove(playerId);
                }
            }
        });
    }

    /**
     * Creates a new SoundTrack builder
     */
    public SoundTrack.Builder createTrack() {
        return SoundTrack.builder().soundManager(this);
    }

    /**
     * Creates a new AmbientSoundPool builder
     */
    public AmbientSoundPool.Builder createAmbientPool() {
        return AmbientSoundPool.builder().soundManager(this);
    }

    /**
     * Shuts down the sound system
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
