package me.mykindos.betterpvp.clans.clans.fatigue.factor;

import me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigue;
import me.mykindos.betterpvp.clans.clans.fatigue.DeathContext;

/**
 * One pluggable, self-contained signal of recklessness.
 * <p>
 * A factor owns <b>everything</b> about itself: its identity ({@link #getName()}),
 * how reckless a death is on its axis ({@link #evaluate}), and how much that
 * axis should count ({@link #getWeight()}). The
 * {@link me.mykindos.betterpvp.clans.clans.fatigue.BattleFatigueManager}
 * discovers implementations via a Guice {@code Multibinder} and combines them
 * generically — it never names or special-cases a concrete factor. Adding a
 * factor is therefore: write the class, add one binding line. No other file
 * changes.
 * <p>
 * Implementations must be <b>pure</b>: derive everything from the supplied
 * {@code state} and {@code context}, never query Bukkit. This keeps the scoring
 * layer deterministic and unit-testable.
 */
public interface FatigueFactor {

    /**
     * @return a stable identifier (used for logging/debugging; the manager does
     * not switch on it).
     */
    String getName();

    /**
     * How much this axis contributes relative to the others. The manager
     * multiplies the normalized {@link #evaluate} output by this. Keep the
     * backing value in a {@code @Config} on the implementation so weights stay
     * tunable and co-located with the factor's logic.
     *
     * @return this factor's weight (typical range ~5–15; non-negative)
     */
    double getWeight();

    /**
     * Evaluate this factor for a death that has just occurred.
     *
     * @param state   the victim's fatigue state, with history <i>not yet</i> including this death
     * @param context the death snapshot
     * @return a normalized magnitude in {@code [0.0, 1.0]} — 0 means "no recklessness
     * signal", 1 means "maximally reckless on this axis". The manager scales by weight.
     */
    double evaluate(BattleFatigue state, DeathContext context);
}
