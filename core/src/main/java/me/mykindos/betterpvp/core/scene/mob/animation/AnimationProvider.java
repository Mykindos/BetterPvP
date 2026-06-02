package me.mykindos.betterpvp.core.scene.mob.animation;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a logical {@link MobAnimation} request into the concrete ModelEngine clip id to play,
 * evaluated against the mob's <b>live</b> state at the moment {@link AnimationController#play} is
 * called. This is the seam that lets a single logical state ({@code HURT}, {@code WALK}, ...) map to
 * something richer than one fixed clip:
 * <ul>
 *   <li>one fixed clip - {@link AnimationProviders#fixed(String)} (the plain-string case)</li>
 *   <li>a random pick from a set - {@link AnimationProviders#random(String...)} (hurt1..hurt4)</li>
 *   <li>a deterministic cycle - {@link AnimationProviders#sequential(String...)}</li>
 *   <li>state-dependent variation - {@link AnimationProviders#when} /
 *       {@link AnimationProviders#whenTargeting} (idle vs idle_combat, hurt vs hurtbad)</li>
 * </ul>
 * Providers compose: a {@code when} branch can itself hold a {@code random}, so "a random hurtbad
 * clip while low on health, otherwise a random hurt clip" is just nesting two helpers.
 * <p>
 * Configure one per state via {@link SceneMob#setAnimation(MobAnimation, AnimationProvider)}. Most
 * mobs only ever need the {@link SceneMob#setAnimation(MobAnimation, String) string overload}, which
 * wraps the id in {@link AnimationProviders#fixed}.
 */
@FunctionalInterface
public interface AnimationProvider {

    /**
     * @param mob the mob the animation is being played on, for state-dependent decisions
     * @return the ModelEngine clip id to play, or {@code null} to play nothing for this request
     *         (the controller then falls back to any built-in cue for the state, e.g. the vanilla
     *         attack flash for {@link MobAnimation#ATTACK}).
     */
    @Nullable String resolve(SceneMob mob);

    /**
     * Resolves with a hint about <i>why</i> the controller is asking, so a held looping state can be
     * refreshed every tick (to pick up live condition changes) without re-rolling a non-deterministic
     * pick on every refresh - which would restart the clip each tick and leave it stuck on its first
     * frame. Two contexts:
     * <ul>
     *   <li>{@code reentry == true} - an explicit {@link AnimationController#play play}/
     *       {@link AnimationController#force force} request: this is a fresh entry into the state, so
     *       non-deterministic providers re-roll ({@code random}) or advance ({@code sequential}).</li>
     *   <li>{@code reentry == false} - a routine {@link AnimationController#tick() tick} refresh of the
     *       held looping state: condition-bearing providers ({@link AnimationProviders#when when},
     *       {@link AnimationProviders#whenTargeting whenTargeting}) re-evaluate so variants swap live,
     *       but non-deterministic leaves return their <i>last</i> pick so the clip keeps looping.</li>
     * </ul>
     * The default treats every call as a re-entry, which is correct for stateless providers
     * ({@link AnimationProviders#fixed fixed}, raw lambdas); stateful providers override it.
     */
    @Nullable
    default String resolve(SceneMob mob, boolean reentry) {
        return resolve(mob);
    }

}
