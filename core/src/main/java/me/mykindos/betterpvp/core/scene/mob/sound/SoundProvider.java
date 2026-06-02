package me.mykindos.betterpvp.core.scene.mob.sound;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a logical {@link MobSound} cue into the concrete {@link SoundEffect} to play, evaluated
 * against the mob's <b>live</b> state at the moment {@link MobSoundBehavior#play(MobSound)} is called.
 * This is the audio counterpart of
 * {@link me.mykindos.betterpvp.core.scene.mob.animation.AnimationProvider} - the seam that lets a
 * single logical cue ({@code HURT}, {@code DEATH}, ...) map to something richer than one fixed sound:
 * <ul>
 *   <li>one fixed sound - {@link SoundProviders#fixed(SoundEffect)} (the plain case)</li>
 *   <li>a random pick from a set - {@link SoundProviders#random(SoundEffect...)} (hurt1..hurt3)</li>
 *   <li>a deterministic cycle - {@link SoundProviders#sequential(SoundEffect...)}</li>
 *   <li>state-dependent variation - {@link SoundProviders#when} /
 *       {@link SoundProviders#whenTargeting} (a snarl in combat, a soft idle grunt otherwise)</li>
 *   <li>per-play pitch jitter - {@link SoundProviders#withPitchVariation} so repeats never sound
 *       mechanically identical</li>
 * </ul>
 * Providers compose: a {@code when} branch can itself hold a {@code random}, and any provider can be
 * wrapped in {@code withPitchVariation}, so "a randomly-pitched random hurt sound while low on health"
 * is just nesting helpers.
 * <p>
 * Configure one per cue via {@link SceneMob#setSound(MobSound, SoundProvider)}. Most mobs only ever
 * need the {@link SceneMob#setSound(MobSound, SoundEffect) SoundEffect overload}, which wraps the
 * effect in {@link SoundProviders#fixed}.
 */
@FunctionalInterface
public interface SoundProvider {

    /**
     * @param mob the mob the sound is being played for, for state-dependent decisions
     * @return the {@link SoundEffect} to play, or {@code null} to play nothing for this request.
     */
    @Nullable SoundEffect resolve(SceneMob mob);

}
