package me.mykindos.betterpvp.clans.clans.leveling;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.events.ClanGainExperienceEvent;
import me.mykindos.betterpvp.clans.clans.leveling.contribution.ClanXpContributionRepository;
import me.mykindos.betterpvp.clans.clans.leveling.events.ClanLevelUpEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
    private final Map<UUID, Long> xpContributions = new ConcurrentHashMap<>();
    private final ClanXpContributionRepository contributionRepository = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ClanXpContributionRepository.class);

    /**
     * Retrieves the current experience of the clan.
     * If the experience property is not set, this method returns 0 as the default value.
     *
     * @return the experience value of the clan as a double. If not defined, returns 0.
     */
    public long getXp() {
        return (long) this.clan.getProperty(ClanProperty.EXPERIENCE).orElse(0L);
    }

    /**
     * Sets the experience value for the clan.
     *
     * @param experience the new experience value to assign to the clan
     */
    public void setXp(final long experience) {
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
    public void grantXp(final long experience, final String reason) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than 0");
        if (!new ClanGainExperienceEvent(clan, null, experience, reason).callEvent()) {
            return;
        }
        long levelBefore = getLevel();
        this.clan.saveProperty(ClanProperty.EXPERIENCE.name(), this.getXp() + experience);

        if (getLevel() > levelBefore) {
            UtilServer.callEvent(new ClanLevelUpEvent(clan, levelBefore, getLevel()));
        }
    }

    /**
     * Grants additional experience to the clan and records the contribution
     * made by the given player.
     *
     * @param actor the player who is contributing the experience
     * @param experience the amount of experience to be granted to the clan.
     *                   Must be a positive value greater than 0
     */
    public void grantXp(final Player actor, final long experience, final String reason) {
        Preconditions.checkArgument(experience > 0, "Experience must be greater than 0");
        if (!new ClanGainExperienceEvent(clan, actor, experience, reason).callEvent()) {
            return; // Event was canceled, do not grant XP
        }
        long levelBefore = getLevel();
        this.clan.saveProperty(ClanProperty.EXPERIENCE.name(), this.getXp() + experience);
        addContribution(actor.getUniqueId(), experience);

        long levelAfter = getLevel();
        if (levelAfter > levelBefore) {
            UtilServer.callEvent(new ClanLevelUpEvent(clan, levelBefore, levelAfter));
        }
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
    private void addContribution(UUID memberUuid, long amount) {
        xpContributions.merge(memberUuid, amount, Long::sum);
        contributionRepository.saveContribution(clan, memberUuid, amount);
    }

    /**
     * Returns the total XP contributed by the given member, or 0 if none recorded.
     */
    public long getContribution(UUID memberUuid) {
        return xpContributions.getOrDefault(memberUuid, 0L);
    }

    /**
     * Returns an unmodifiable view of all per-member XP contributions.
     */
    public Map<UUID, Long> getContributions() {
        return Collections.unmodifiableMap(xpContributions);
    }

    /**
     * Bulk-loads contribution data (called once by the repository during startup).
     */
    public void loadContributions(Map<UUID, Long> data) {
        xpContributions.putAll(data);
    }

    /**
     * Returns the total cumulative XP required to reach the given level.
     */
    public static long cumulativeXpForLevel(long level) {
        return (long) (Math.pow(level, 2) * scale);
    }

    /**
     * Computes the level a clan is at given its total accumulated XP.
     */
    public static long levelFromXp(long totalXp) {
        if (totalXp <= 0) return 0;
        return (long) Math.pow(totalXp / (double) scale, 0.5);
    }

    /**
     * Returns the XP a clan has accumulated within its current level
     * (i.e. progress towards the next level).
     */
    public static long xpInCurrentLevel(long currentLevel, long totalXp) {
        return totalXp - cumulativeXpForLevel(currentLevel);
    }

    /**
     * Returns the total XP required to advance from the given level to the next.
     */
    public static long xpRequiredForNextLevel(long currentLevel) {
        return cumulativeXpForLevel(currentLevel + 1) - cumulativeXpForLevel(currentLevel);
    }


}
