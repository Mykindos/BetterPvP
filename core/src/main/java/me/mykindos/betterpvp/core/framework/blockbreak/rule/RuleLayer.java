package me.mykindos.betterpvp.core.framework.blockbreak.rule;

/**
 * Composition layer for a {@link BlockBreakRule}. The resolver folds matching
 * rules in a fixed order:
 * <pre>
 *   final = override.orElse((base + Σ additive) × Π multiplier)
 * </pre>
 * Any rule whose properties are {@code !breakable} short-circuits to unbreakable
 * regardless of layer.
 */
public enum RuleLayer {
    /** Flat speed added to the base. Default for legacy rules. */
    ADDITIVE,
    /** Multiplier applied to {@code base + additive}. Multiple multipliers compose by product. */
    MULTIPLICATIVE,
    /** Forces the final speed, ignoring additive/multiplicative contributions. Highest {@code priority()} wins. */
    OVERRIDE
}
