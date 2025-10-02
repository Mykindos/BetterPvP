package me.mykindos.betterpvp.core.loot;

/**
 * Represents the strategy to use when a loot entry is replaced.
 */
public enum ReplacementStrategy {
    UNSET,
    WITH_REPLACEMENT,
    WITHOUT_REPLACEMENT;

    public ReplacementStrategy orElse(ReplacementStrategy other) {
        return this == UNSET ? other : this;
    }
}