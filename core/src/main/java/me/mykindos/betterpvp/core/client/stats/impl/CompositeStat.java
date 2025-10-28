package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

//todo a stat that just sums other stats from a container
public class CompositeStat implements IStat {
    private final Set<IStat> stats = new HashSet<>();
    @Getter
    private final String statName;

    public CompositeStat(String statName, IStat... stats) {
        this(statName, Set.of(stats));
    }

    public CompositeStat(String statName, Set<IStat> stats) {
        this.statName = statName;
        this.stats.addAll(stats);
    }


    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param periodKey
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String periodKey) {
       return stats.stream()
               .mapToDouble(stat -> stat.getStat(statContainer, periodKey))
               .sum();
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
        return statName;
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

    //todo check logic here
    @Override
    public boolean containsStat(String statName) {
        return stats.stream().anyMatch(stat -> stat.containsStat(statName));
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
