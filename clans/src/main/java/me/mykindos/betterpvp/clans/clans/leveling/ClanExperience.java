package me.mykindos.betterpvp.clans.clans.leveling;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanProperty;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
@RequiredArgsConstructor
public class ClanExperience {

    private static final long scale = 1000L; // Target level
    private final Clan clan;

    /**
     * Per-member XP contribution tracking. Keys are UUID strings; values are total XP contributed.
     */
    private final Map<UUID, Double> xpContributions = new ConcurrentHashMap<>();

    /**
     * Retrieves the current experience of the clan.
     * If the experience property is not set, this method returns 0 as the default value.
     *
     * @return the experience value of the clan as a double. If not defined, returns 0.
     */
    public double getXp() {
        return (double) this.clan.getProperty(ClanProperty.EXPERIENCE).orElse(0d);
    }

    /**
     * Sets the experience value for the clan.
     *
     * @param experience the new experience value to assign to the clan
     */
    public void setXp(final double experience) {
        this.clan.saveProperty(ClanProperty.EXPERIENCE.name(), experience);
    }

    /**
     * Grants additional experience to the clan by adding the specified amount
     * to the current experience value.
     *
     * @param experience the amount of experience to be granted to the clan.
     *                   Must be a positive value greater than 0.
     * @throws IllegalArgumentException if the provided experience value is less than or equal to 0.
     */
    public void grantXp(final double experience) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than 0");
        this.clan.saveProperty(ClanProperty.EXPERIENCE.name(), this.getXp() + experience);
    }

    /**
     * @return  the current level of the clan.
     */
    public long getLevel() {
        return levelFromXp(getXp());
    }

    /**
     * Records an XP contribution for the given member.
     */
    public void addContribution(UUID memberUuid, double amount) {
        xpContributions.merge(memberUuid, amount, Double::sum);
    }

    /**
     * Returns the total XP contributed by the given member, or 0 if none recorded.
     */
    public double getContribution(UUID memberUuid) {
        return xpContributions.getOrDefault(memberUuid, 0.0);
    }

    /**
     * Returns an unmodifiable view of all per-member XP contributions.
     */
    public Map<UUID, Double> getContributions() {
        return Collections.unmodifiableMap(xpContributions);
    }

    /**
     * Bulk-loads contribution data (called once by the repository during startup).
     */
    public void loadContributions(Map<UUID, Double> data) {
        xpContributions.putAll(data);
    }

    /**
     * Returns the total cumulative XP required to reach the given level.
     */
    public static double cumulativeXpForLevel(long level) {
        return Math.pow(level, 3) / scale;
    }

    /**
     * Computes the level a clan is at given its total accumulated XP.
     */
    public static long levelFromXp(double totalXp) {
        if (totalXp <= 0) return 0;
        return (long) Math.cbrt(totalXp * scale);
    }

    /**
     * Returns the XP a clan has accumulated within its current level
     * (i.e. progress towards the next level).
     */
    public static double xpInCurrentLevel(long currentLevel, double totalXp) {
        return totalXp - cumulativeXpForLevel(currentLevel);
    }

    /**
     * Returns the total XP required to advance from the given level to the next.
     */
    public static double xpRequiredForNextLevel(long currentLevel) {
        return cumulativeXpForLevel(currentLevel + 1) - cumulativeXpForLevel(currentLevel);
    }


}
