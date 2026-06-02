package me.mykindos.betterpvp.core.scene.mob.sound;

import lombok.Getter;

/**
 * Logical sound cues a {@link me.mykindos.betterpvp.core.scene.mob.SceneMob} can emit. AI components
 * and combat listeners request these cues; the {@link MobSoundBehavior} maps each to a concrete
 * {@link me.mykindos.betterpvp.core.utilities.model.SoundEffect} via a {@link SoundProvider} configured
 * on the mob (and no-ops if no provider is set for the cue).
 * <p>
 * This is the audio twin of {@link me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation}: cues
 * are either <b>ambient</b> or <b>one-shot</b>, which is what tells the behaviour how to drive them:
 * <ul>
 *   <li><b>Ambient</b> cues (IDLE) are emitted on a slow, jittered timer while the mob is active - the
 *       background growls/breathing that make a mob feel alive without being requested.</li>
 *   <li><b>One-shot</b> cues (ATTACK, HURT, DEATH) are fired once in reaction to an event, beside the
 *       matching {@link me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation}.</li>
 * </ul>
 */
@Getter
public enum MobSound {
    /** Background bed - growls/breathing emitted periodically while the mob is active. */
    IDLE(true),
    ATTACK(false),
    HURT(false),
    DEATH(false);

    /** Whether this cue is emitted on the ambient timer (vs. fired once in reaction to an event). */
    private final boolean ambient;

    MobSound(boolean ambient) {
        this.ambient = ambient;
    }
}
