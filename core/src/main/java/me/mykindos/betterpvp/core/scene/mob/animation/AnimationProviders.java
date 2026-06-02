package me.mykindos.betterpvp.core.scene.mob.animation;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.utilities.UtilMath;

import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * Ready-made {@link AnimationProvider}s and combinators for the common clip-selection strategies.
 * These are building blocks - they compose, so richer behaviour is expressed by nesting rather than
 * by writing a bespoke provider:
 * <pre>{@code
 * // A random hurt clip normally, a random "bad hurt" clip while below 25% health.
 * setAnimation(MobAnimation.HURT, AnimationProviders.when(
 *         mob -> mob.getEntity().getHealth() / mob.getEntity().getMaxHealth() < 0.25,
 *         AnimationProviders.random("hurtbad1", "hurtbad2", "hurtbad3", "hurtbad4"),
 *         AnimationProviders.random("hurt1", "hurt2", "hurt3", "hurt4")));
 *
 * // Combat-aware locomotion.
 * setAnimation(MobAnimation.WALK, AnimationProviders.whenTargeting(
 *         AnimationProviders.fixed("walk_combat"),
 *         AnimationProviders.sequential("walk", "walk2")));
 * }</pre>
 */
public final class AnimationProviders {

    private AnimationProviders() {
    }

    /** Always resolves to the same clip - the equivalent of the plain-string mapping. */
    public static AnimationProvider fixed(String animationId) {
        Preconditions.checkNotNull(animationId, "animationId cannot be null");
        return mob -> animationId;
    }

    /**
     * Resolves to a uniformly random clip from {@code animationIds}. A fresh pick is rolled on each
     * explicit (re)entry; a routine tick refresh of a held looping state returns the last pick so the
     * chosen clip keeps looping instead of re-rolling - and restarting - every tick.
     */
    public static AnimationProvider random(String... animationIds) {
        Preconditions.checkArgument(animationIds.length > 0, "At least one animation id must be provided");
        return picking(animationIds, current -> UtilMath.randomInt(animationIds.length));
    }

    /**
     * Resolves to each clip in turn, wrapping back to the first after the last - useful for combo
     * chains (hurt1 -> hurt2 -> hurt3 -> hurt4 -> hurt1 -> ...). The cursor advances only on an explicit
     * (re)entry; a routine tick refresh of a held looping state returns the current clip without
     * advancing, so a looping sequential clip keeps playing rather than stepping every tick. The cursor
     * is per-provider state, so give each mob its own instance (which {@code setAnimation} does
     * naturally).
     */
    public static AnimationProvider sequential(String... animationIds) {
        Preconditions.checkArgument(animationIds.length > 0, "At least one animation id must be provided");
        return picking(animationIds, current -> (current + 1) % animationIds.length);
    }

    /**
     * Shared backbone for the stateful multi-clip strategies ({@link #random}, {@link #sequential}):
     * holds the last pick and only advances - via {@code nextIndex}, given the current cursor - on an
     * explicit (re)entry, so a held looping state keeps the same clip across routine tick refreshes
     * instead of re-rolling/stepping every tick. (Sound providers have no equivalent because they are
     * stateless point events, not held looping states.)
     */
    private static AnimationProvider picking(String[] animationIds, IntUnaryOperator nextIndex) {
        return new AnimationProvider() {
            private int index = -1; // pre-first: the first advance yields the starting index (0 for sequential)
            private String last;

            @Override
            public String resolve(SceneMob mob) {
                return resolve(mob, true);
            }

            @Override
            public String resolve(SceneMob mob, boolean reentry) {
                if (!reentry && last != null) {
                    return last;
                }
                index = nextIndex.applyAsInt(index);
                last = animationIds[index];
                return last;
            }
        };
    }

    /**
     * Picks between two providers based on the mob's live state at play time. Either branch may be
     * any provider, including another {@code when}, so conditions can be chained. The condition is
     * re-evaluated on every resolution (including routine tick refreshes), so the active variant swaps
     * live; the {@code reentry} hint is threaded into the chosen branch so its non-deterministic leaves
     * stay stable across ticks.
     */
    public static AnimationProvider when(Predicate<SceneMob> condition, AnimationProvider ifTrue, AnimationProvider ifFalse) {
        Preconditions.checkNotNull(condition, "condition cannot be null");
        return new AnimationProvider() {
            @Override
            public String resolve(SceneMob mob) {
                return resolve(mob, true);
            }

            @Override
            public String resolve(SceneMob mob, boolean reentry) {
                return (condition.test(mob) ? ifTrue : ifFalse).resolve(mob, reentry);
            }
        };
    }

    /** Single-clip shorthand for {@link #when(Predicate, AnimationProvider, AnimationProvider)}. */
    public static AnimationProvider when(Predicate<SceneMob> condition, String ifTrue, String ifFalse) {
        return when(condition, fixed(ifTrue), fixed(ifFalse));
    }

    /**
     * Convenience over {@link #when}: uses {@code targeting} while the mob has a current target
     * (i.e. is in combat), otherwise {@code idle}. Covers the idle/idle_combat, walk/walk_combat
     * style splits.
     */
    public static AnimationProvider whenTargeting(AnimationProvider targeting, AnimationProvider idle) {
        return when(mob -> mob.getCurrentTarget() != null, targeting, idle);
    }

    /** Single-clip shorthand for {@link #whenTargeting(AnimationProvider, AnimationProvider)}. */
    public static AnimationProvider whenTargeting(String targeting, String idle) {
        return whenTargeting(fixed(targeting), fixed(idle));
    }

}
