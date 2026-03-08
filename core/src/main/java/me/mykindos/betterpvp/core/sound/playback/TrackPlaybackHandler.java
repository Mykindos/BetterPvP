package me.mykindos.betterpvp.core.sound.playback;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.sound.model.SoundCategory;
import me.mykindos.betterpvp.core.sound.model.SoundTrack;
import me.mykindos.betterpvp.core.sound.model.SoundTrackInstance;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles playback logic for music tracks (intro -> loop -> finale)
 */
@CustomLog
public class TrackPlaybackHandler {

    private final SoundPlayer soundPlayer;
    private final ScheduledExecutorService executorService;
    private final TrackCleanupCallback cleanupCallback;

    public TrackPlaybackHandler(SoundPlayer soundPlayer, ScheduledExecutorService executorService,
                               TrackCleanupCallback cleanupCallback) {
        this.soundPlayer = soundPlayer;
        this.executorService = executorService;
        this.cleanupCallback = cleanupCallback;
    }

    /**
     * Starts playback of a sound track
     */
    public void startPlayback(SoundTrackInstance instance) {
        if (instance.isStopped()) {
            return;
        }

        SoundTrack track = instance.getTrack();

        // Play intro if available
        if (track.getIntroSound() != null && instance.getCurrentPhase().get() == SoundTrackInstance.TrackPhase.INTRO) {
            playSound(instance, track.getIntroSound());

            // Schedule transition to loop phase using configured duration
            ScheduledFuture<?> task = executorService.schedule(() -> {
                instance.setPhase(SoundTrackInstance.TrackPhase.LOOP);
                startLoopPhase(instance);
            }, track.getIntroDuration(), TimeUnit.SECONDS);

            instance.setCurrentTask(task);
        } else {
            // No intro, go straight to loop
            instance.setPhase(SoundTrackInstance.TrackPhase.LOOP);
            startLoopPhase(instance);
        }
    }

    /**
     * Starts the loop phase of a sound track
     */
    private void startLoopPhase(SoundTrackInstance instance) {
        if (instance.isStopped()) {
            return;
        }

        SoundTrack track = instance.getTrack();

        // Check if finale was triggered
        if (instance.isFinaleTriggered()) {
            playFinale(instance);
            return;
        }

        // Play loop sound
        if (track.getLoopSound() != null) {
            playSound(instance, track.getLoopSound());

            // Check if we should loop
            if (track.isShouldLoop()) {
                // Schedule next loop iteration using configured duration
                ScheduledFuture<?> task = executorService.schedule(() -> {
                    startLoopPhase(instance); // Recursive call for looping
                }, track.getLoopDuration(), TimeUnit.SECONDS);

                instance.setCurrentTask(task);
            } else {
                // Single play-through, stop after duration
                executorService.schedule(() -> {
                    instance.stop();
                    cleanupCallback.onTrackComplete(instance);
                }, track.getLoopDuration(), TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Plays the finale phase of a sound track
     */
    private void playFinale(SoundTrackInstance instance) {
        if (instance.isStopped()) {
            return;
        }

        SoundTrack track = instance.getTrack();

        if (track.getFinaleSound() != null) {
            instance.setPhase(SoundTrackInstance.TrackPhase.FINALE);
            playSound(instance, track.getFinaleSound());

            // After finale completes, mark as stopped using configured duration
            executorService.schedule(() -> {
                instance.stop();
                cleanupCallback.onTrackComplete(instance);
            }, track.getFinaleDuration(), TimeUnit.SECONDS);
        } else {
            // No finale sound, just stop immediately
            instance.stop();
            cleanupCallback.onTrackComplete(instance);
        }
    }

    /**
     * Plays a sound for a track instance
     */
    private void playSound(SoundTrackInstance instance, String soundKey) {
        SoundTrack track = instance.getTrack();
        soundPlayer.playSound(
            instance.getPlayerIds(),
            soundKey,
            track.getVolume(),
            track.getPitch(),
            SoundCategory.MUSIC,
            instance.getLocation()
        );
    }

    /**
     * Callback interface for track cleanup
     */
    public interface TrackCleanupCallback {
        void onTrackComplete(SoundTrackInstance instance);
    }

}
