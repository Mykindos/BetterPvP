package me.mykindos.betterpvp.clans.clans.leveling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.Config;

/**
 * Configurable XP formula service for the clan leveling system.
 *
 * <p>Formula: {@code cumulativeXp(L) = L^3 / scale}
 * which inverts to: {@code level(xp) = floor(cbrt(xp * scale))}
 *
 * <p>Default values target level 1000 at 1,000,000 total cumulative XP:
 * <ul>
 *   <li>Level 100  =      1,000 XP</li>
 *   <li>Level 500  =    125,000 XP</li>
 *   <li>Level 1000 =  1,000,000 XP</li>
 * </ul>
 */
@Singleton
public class ClanXpFormula {

    @Inject
    @Getter
    @Config(path = "clans.leveling.formula.scale", defaultValue = "1000.0")
    private double scale;

    /**
     * Returns the total cumulative XP required to reach the given level.
     */
    public double cumulativeXpForLevel(long level) {
        return Math.pow(level, 3) / scale;
    }

    /**
     * Computes the level a clan is at given its total accumulated XP.
     */
    public long levelFromXp(double totalXp) {
        if (totalXp <= 0) return 0;
        return (long) Math.cbrt(totalXp * scale);
    }

    /**
     * Returns the XP a clan has accumulated within its current level
     * (i.e. progress towards the next level).
     */
    public double xpInCurrentLevel(long currentLevel, double totalXp) {
        return totalXp - cumulativeXpForLevel(currentLevel);
    }

    /**
     * Returns the total XP required to advance from the given level to the next.
     */
    public double xpRequiredForNextLevel(long currentLevel) {
        return cumulativeXpForLevel(currentLevel + 1) - cumulativeXpForLevel(currentLevel);
    }

}
