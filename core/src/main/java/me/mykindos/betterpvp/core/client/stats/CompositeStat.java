package me.mykindos.betterpvp.core.client.stats;

import lombok.Getter;

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
     * @param period
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
       return stats.stream()
               .mapToDouble(stat -> stat.getStat(statContainer, period))
               .sum();
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

    @Override
    public boolean containsStat(String statName) {
        return stats.stream().anyMatch(stat -> stat.containsStat(statName));
    }
}
