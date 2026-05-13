package me.mykindos.betterpvp.core.loot.expression;

/**
 * Functions exposed to loot expressions in the default namespace.
 * <p>
 * Methods are invoked from expressions as bare names, e.g. {@code clamp(x, 1, 5)}.
 */
public final class LootFunctions {

    /** Returns {@code v} clamped between {@code lo} and {@code hi}. */
    public double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Returns the smaller of {@code a} and {@code b}. */
    public double min(double a, double b) {
        return Math.min(a, b);
    }

    /** Returns the larger of {@code a} and {@code b}. */
    public double max(double a, double b) {
        return Math.max(a, b);
    }

    /** Returns {@code a} rounded to the nearest integer (as a long). */
    public long round(double a) {
        return Math.round(a);
    }

    /** Returns the floor of {@code a}. */
    public double floor(double a) {
        return Math.floor(a);
    }

    /** Returns the ceiling of {@code a}. */
    public double ceil(double a) {
        return Math.ceil(a);
    }

    /** Returns the absolute value of {@code a}. */
    public double abs(double a) {
        return Math.abs(a);
    }

    /** Returns the length of {@code s}, or 0 if null. */
    public int len(String s) {
        return s == null ? 0 : s.length();
    }
}
