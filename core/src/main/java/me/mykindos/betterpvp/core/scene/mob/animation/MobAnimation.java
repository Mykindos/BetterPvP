package me.mykindos.betterpvp.core.scene.mob.animation;

import lombok.Getter;

/**
 * Logical animation states a {@link me.mykindos.betterpvp.core.scene.mob.SceneMob} can be in.
 * AI components request these states; the {@link AnimationController} maps each to the concrete
 * ModelEngine animation id via an {@link AnimationProvider} configured on the mob (and no-ops if
 * the mob has no bound model).
 * <p>
 * Each state is either <b>looping</b> or <b>one-shot</b>, which is what tells the controller how to
 * treat it:
 * <ul>
 *   <li><b>Looping</b> states (locomotion/posture - IDLE, WALK) are <i>held</i>: the controller
 *       remembers the current looping state and re-resolves its provider every tick, swapping the
 *       underlying clip only when the resolved id changes. This is what makes state-dependent
 *       variations (walk &rarr; walk_combat) switch live without restarting the clip each tick.</li>
 *   <li><b>One-shot</b> states (reactions - ATTACK, HURT, DEATH) play once over whatever is looping
 *       and are not remembered.</li>
 * </ul>
 */
@Getter
public enum MobAnimation {
    IDLE(true),
    WALK(true),
    ATTACK(false),
    HURT(false),
    DEATH(false); // played when the entity is removed by ModelEngine itself

    /** Whether this state is held and continuously re-resolved (vs. fired once and forgotten). */
    private final boolean looping;

    MobAnimation(boolean looping) {
        this.looping = looping;
    }
}
