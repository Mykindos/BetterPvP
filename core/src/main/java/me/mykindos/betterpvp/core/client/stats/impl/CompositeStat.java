package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.server.Period;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

//todo a stat that just sums other stats from a container
public class CompositeStat implements IStat {
    private final Set<IStat> stats = new HashSet<>();
    @Getter
    private final String statType;

    public CompositeStat(String statType, IStat... stats) {
        this(statType, Set.of(stats));
    }

    public CompositeStat(String statType, Set<IStat> stats) {
        this.statType = statType;
        this.stats.addAll(stats);
    }


    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
       return stats.stream()
               .mapToLong(stat -> stat.getStat(statContainer, type, period))
               .sum();
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return null;
    }

    /**
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        return statType;
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return false;
    }

    /**
     * Whether this stat contains this otherSTat
     *
     * @param otherStat
     * @return
     */
    @Override
    public boolean containsStat(IStat otherStat) {
        return stats.stream().anyMatch(stat -> stat.containsStat(otherStat));
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        return this;
    }
}
