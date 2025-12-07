package me.mykindos.betterpvp.core.sound.model;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle for an active ambient sound pool instance
 * Allows control over the ambient sound playback
 */
@Getter
public class AmbientSoundInstance {

    private final UUID instanceId;
    private final Set<UUID> playerIds;
    private final Location location;
    private final AmbientSoundPool pool;
    private final AtomicBoolean paused;
    private final AtomicBoolean stopped;
    private ScheduledFuture<?> currentTask;

    public AmbientSoundInstance(AmbientSoundPool pool, Set<Player> players, Location location) {
        this.instanceId = UUID.randomUUID();
        this.pool = pool;
        this.playerIds = ConcurrentHashMap.newKeySet();
        players.forEach(player -> this.playerIds.add(player.getUniqueId()));
        this.location = location;
        this.paused = new AtomicBoolean(false);
        this.stopped = new AtomicBoolean(false);
    }

    /**
     * Stops the ambient sound completely
     */
    public void stop() {
        stopped.set(true);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(false);
        }
    }

    /**
     * Pauses the ambient sound temporarily
     */
    public void pause() {
        if (!stopped.get()) {
            paused.set(true);
        }
    }

    /**
     * Resumes the ambient sound if paused
     */
    public void resume() {
        if (!stopped.get() && paused.get()) {
            paused.set(false);
        }
    }

    /**
     * Checks if the ambient sound is paused
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * Checks if the ambient sound has been stopped
     *
     * @return true if stopped, false otherwise
     */
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * Updates the current scheduled task
     *
     * @param task The new scheduled task
     */
    public void setCurrentTask(ScheduledFuture<?> task) {
        this.currentTask = task;
    }

    /**
     * Checks if this instance is location-based
     *
     * @return true if location-based, false if player-following
     */
    public boolean isLocationBased() {
        return location != null;
    }

    /**
     * Removes a player from this instance
     *
     * @param playerId The player UUID to remove
     * @return true if there are still players remaining, false if empty
     */
    public boolean removePlayer(UUID playerId) {
        playerIds.remove(playerId);
        return !playerIds.isEmpty();
    }

}
