package me.mykindos.betterpvp.core.scene.mob.sound;

import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Range;

import java.util.Map;

/**
 * The voice of a {@link SceneMob}: maps logical {@link MobSound} cues to concrete
 * {@link SoundEffect}s via per-cue {@link SoundProvider}s and emits them at the mob's location. It is
 * the audio counterpart of {@link me.mykindos.betterpvp.core.scene.mob.animation.AnimationController},
 * but realised as a {@link SceneBehavior} because it has two jobs:
 * <ul>
 *   <li><b>Push (one-shot cues)</b> - combat listeners and AI components call {@link #play(MobSound)}
 *       beside the matching animation (e.g. {@code play(MobSound.HURT)} when the mob is struck).</li>
 *   <li><b>Pull (ambient cues)</b> - {@link #tick()} emits any {@linkplain MobSound#isAmbient() ambient}
 *       cue (IDLE) on a slow, jittered timer while the mob is {@linkplain SceneMob#isActive() active},
 *       so the mob breathes/growls on its own without anything having to ask.</li>
 * </ul>
 * Resolution happens against the mob's <b>live</b> state, so providers like
 * {@link SoundProviders#whenTargeting} can swap the idle bed for a combat snarl the moment a target is
 * acquired. A cue with no provider is silently skipped, so callers never need to check.
 * <p>
 * The ambient timer defaults to a 5-15 second jittered gap that always fires; its bounds and per-beat
 * probability can be tuned fluently, mirroring the AI components:
 * {@code mob.getSounds().minAmbientTicks(60).maxAmbientTicks(200).ambientChance(0.7)}. Created and
 * attached by {@link SceneMob}; reachable as {@link SceneMob#getSounds()}.
 */
@Accessors(fluent = true, chain = true)
public class MobSoundBehavior implements SceneBehavior {

    private final SceneMob mob;
    private final Map<MobSound, SoundProvider> providers;

    /** Shortest gap, in ticks, between ambient emissions (lower bound of the jitter window). */
    @Setter
    @Range(from = 1, to = Integer.MAX_VALUE)
    private int minAmbientTicks = 100;
    /** Longest gap, in ticks, between ambient emissions (upper bound of the jitter window). */
    @Setter
    @Range(from = 1, to = Integer.MAX_VALUE)
    private int maxAmbientTicks = 300;
    /** Probability in {@code [0, 1]} that an ambient cue actually fires when the timer elapses. */
    @Setter
    @Range(from = 0, to = 1)
    private double ambientChance = 1.0;

    /** Ticks remaining until the next ambient emission; reset to a fresh jittered interval each time. */
    private int ambientCountdown;

    public MobSoundBehavior(SceneMob mob, Map<MobSound, SoundProvider> providers) {
        this.mob = mob;
        this.providers = providers;
        this.ambientCountdown = nextAmbientInterval();
    }

    /**
     * Emits a one-shot cue now. Resolves the cue's provider against the mob's live state and plays the
     * resulting effect at the mob's location. No-op if the cue has no provider, resolves to nothing,
     * or the mob isn't in the world yet.
     */
    public void play(MobSound sound) {
        emit(providers.get(sound));
    }

    /**
     * Drives the ambient cues. While the mob is active, counts down a jittered timer and, when it
     * elapses, emits every ambient cue the mob has a provider for (subject to {@link #ambientChance}).
     * Skipped entirely while the mob is out of activation range so dormant mobs stay silent and cheap.
     */
    @Override
    public void tick() {
        if (!mob.isActive()) {
            return;
        }
        if (--ambientCountdown > 0) {
            return;
        }
        ambientCountdown = nextAmbientInterval();
        if (ambientChance < 1.0 && UtilMath.RANDOM.nextDouble() >= ambientChance) {
            return; // timer elapsed but this beat is skipped, keeping the bed irregular
        }
        for (Map.Entry<MobSound, SoundProvider> entry : providers.entrySet()) {
            if (entry.getKey().isAmbient()) {
                emit(entry.getValue());
            }
        }
    }

    private void emit(SoundProvider provider) {
        if (provider == null || !mob.isInitialized()) {
            return;
        }
        final SoundEffect effect = provider.resolve(mob);
        if (effect != null) {
            final ModeledEntity modeledEntity = mob.getModeledEntity();
            final Location location = modeledEntity == null ? mob.getEntity().getLocation() : modeledEntity.getBase().getLocation();
            effect.play(location);
        }
    }

    /**
     * A random gap (in ticks) within the configured window, so a group of mobs doesn't grunt in
     * unison. Tolerates a collapsed or inverted window (min &gt;= max) by returning the lower bound
     * rather than letting {@link UtilMath#randomInt(int, int)} throw.
     */
    private int nextAmbientInterval() {
        return maxAmbientTicks <= minAmbientTicks ? minAmbientTicks : UtilMath.randomInt(minAmbientTicks, maxAmbientTicks);
    }
}
