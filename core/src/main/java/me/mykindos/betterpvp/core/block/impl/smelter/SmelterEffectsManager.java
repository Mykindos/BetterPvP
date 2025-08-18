package me.mykindos.betterpvp.core.block.impl.smelter;

import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.LAVA_AMBIENT_PITCH;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.LAVA_AMBIENT_SOUND_CHANCE;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.LAVA_AMBIENT_VOLUME;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.LAVA_POP_PITCH;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.LAVA_POP_SOUND_CHANCE;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.LAVA_POP_VOLUME;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.PARTICLE_INTERVAL_TICKS;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.PARTICLE_RECEIVER_DISTANCE;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.SMOKE_HEIGHT_OFFSET;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.SMOKE_PARTICLE_COUNT;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.SMOKE_Y_OFFSET;
import static me.mykindos.betterpvp.core.block.impl.smelter.SmelterConstants.SOUND_INTERVAL_TICKS;

/**
 * Manages sound and particle effects for Smelter
 */
public class SmelterEffectsManager {

    /**
     * Plays burning effects (sounds and particles) if the smelter is burning.
     *
     * @param location  The location to play effects at
     * @param isBurning Whether the smelter is currently burning
     */
    public void playBurningEffects(@NotNull Location location, boolean isBurning) {
        if (!isBurning) {
            return;
        }

        final int tick = Bukkit.getCurrentTick();

        // Play sound effects
        if (tick % SOUND_INTERVAL_TICKS == 0) {
            if (Math.random() < LAVA_AMBIENT_SOUND_CHANCE) {
                new SoundEffect(Sound.BLOCK_LAVA_AMBIENT, LAVA_AMBIENT_VOLUME, LAVA_AMBIENT_PITCH).play(location);
            }

            if (Math.random() < LAVA_POP_SOUND_CHANCE) {
                new SoundEffect(Sound.BLOCK_LAVA_POP, LAVA_POP_VOLUME, LAVA_POP_PITCH).play(location);
            }
        }

        // Spawn particles
        if (tick % PARTICLE_INTERVAL_TICKS == 0) {
            spawnSmokeParticles(location);
        }
    }

    /**
     * Spawns smoke particles above the smelter.
     *
     * @param location The base location of the smelter
     */
    private void spawnSmokeParticles(@NotNull Location location) {
        Particle.CAMPFIRE_COSY_SMOKE.builder()
                .location(location.clone().add(0, SMOKE_HEIGHT_OFFSET, 0))
                .count(SMOKE_PARTICLE_COUNT)
                .receivers(PARTICLE_RECEIVER_DISTANCE)
                .offset(0, SMOKE_Y_OFFSET, 0)
                .extra(0)
                .spawn();
    }
} 