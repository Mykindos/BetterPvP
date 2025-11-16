package me.mykindos.betterpvp.core.sound.playback;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.sound.model.AmbientSoundInstance;
import me.mykindos.betterpvp.core.sound.model.AmbientSoundPool;
import me.mykindos.betterpvp.core.sound.model.SoundCategory;
import me.mykindos.betterpvp.core.sound.model.SoundDefinition;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles playback logic for ambient sound pools
 * Randomly selects and plays sounds from a pool at intervals
 */
@CustomLog
public class AmbientPlaybackHandler {

    private final SoundPlayer soundPlayer;
    private final ScheduledExecutorService executorService;

    public AmbientPlaybackHandler(SoundPlayer soundPlayer, ScheduledExecutorService executorService) {
        this.soundPlayer = soundPlayer;
        this.executorService = executorService;
    }

    /**
     * Starts ambient sound playback
     */
    public void startPlayback(AmbientSoundInstance instance) {
        scheduleNextSound(instance);
    }

    /**
     * Schedules the next ambient sound from the pool
     */
    private void scheduleNextSound(AmbientSoundInstance instance) {
        if (instance.isStopped()) {
            return;
        }

        AmbientSoundPool pool = instance.getPool();

        // Calculate random interval
        int randomInterval = calculateRandomInterval(pool.getMinInterval(), pool.getMaxInterval());

        ScheduledFuture<?> task = executorService.schedule(() -> {
            if (!instance.isStopped() && !instance.isPaused()) {
                // Pick and play random sound
                SoundDefinition sound = pool.getRandomSound();
                if (sound != null) {
                    playAmbientSound(instance, sound);
                }
            }

            // Schedule next sound
            scheduleNextSound(instance);
        }, randomInterval, TimeUnit.SECONDS);

        instance.setCurrentTask(task);
    }

    /**
     * Plays an ambient sound
     */
    private void playAmbientSound(AmbientSoundInstance instance, SoundDefinition soundDef) {
        soundPlayer.playSound(
            instance.getPlayerIds(),
            soundDef,
            SoundCategory.AMBIENT,
            instance.getLocation()
        );
    }

    /**
     * Calculates a random interval between min and max
     */
    private int calculateRandomInterval(int minSeconds, int maxSeconds) {
        return minSeconds + (int) (Math.random() * (maxSeconds - minSeconds + 1));
    }

}
