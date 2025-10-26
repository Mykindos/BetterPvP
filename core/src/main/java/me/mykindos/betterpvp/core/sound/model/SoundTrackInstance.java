package me.mykindos.betterpvp.core.sound.model;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handle for an active sound track instance
 * Tracks the current playback state and allows control over the track
 */
@Getter
public class SoundTrackInstance {

    private final UUID instanceId;
    private final Set<UUID> playerIds;
    private final Location location;
    private final AtomicReference<TrackPhase> currentPhase;
    private final AtomicBoolean finaleTriggered;
    private final AtomicBoolean stopped;
    private ScheduledFuture<?> currentTask;
    private final SoundTrack track;

    /**
     * Represents the current phase of the sound track
     */
    public enum TrackPhase {
        INTRO,
        LOOP,
        FINALE,
        STOPPED
    }

    public SoundTrackInstance(SoundTrack track, Set<Player> players, Location location) {
        this.instanceId = UUID.randomUUID();
        this.track = track;
        this.playerIds = ConcurrentHashMap.newKeySet();
        players.forEach(player -> this.playerIds.add(player.getUniqueId()));
        this.location = location;
        this.currentPhase = new AtomicReference<>(TrackPhase.INTRO);
        this.finaleTriggered = new AtomicBoolean(false);
        this.stopped = new AtomicBoolean(false);
    }

    /**
     * Triggers the finale phase of the sound track
     * Will transition to finale after the current loop completes
     */
    public void triggerFinale() {
        if (!stopped.get() && !finaleTriggered.get()) {
            finaleTriggered.set(true);
        }
    }

    /**
     * Stops the sound track immediately
     */
    public void stop() {
        stopped.set(true);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(false);
        }
        currentPhase.set(TrackPhase.STOPPED);
    }

    /**
     * Checks if the finale has been triggered
     *
     * @return true if finale triggered, false otherwise
     */
    public boolean isFinaleTriggered() {
        return finaleTriggered.get();
    }

    /**
     * Checks if the track has been stopped
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
     * Advances to the next phase
     *
     * @param phase The new phase
     */
    public void setPhase(TrackPhase phase) {
        this.currentPhase.set(phase);
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
