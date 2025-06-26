package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.Builder;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Builder
public class DamageStat implements IStat {
    public static String PREFIX = "DAMAGE_";
    public static String RELATION_SUFFIX = "_";

    @Nullable
    private EntityDamageEvent.DamageCause damageCause;
    @NotNull
    private Relation relation;


    /**
     * Get the stat represented by this object from the statContainer
     *
     * @param statContainer
     * @param period
     * @return
     */
    @Override
    public Double getStat(StatContainer statContainer, String period) {
        if (damageCause == null) {
            return statContainer.getStats().getStatsOfPeriod(period).entrySet().stream()
                    .filter(entry ->
                    entry.getKey().startsWith(getStatName())
            ).mapToDouble(Map.Entry::getValue)
                    .sum();
        }
        return statContainer.getProperty(getStatName(), period);
    }

    @Override
    public String getStatName() {
        if (damageCause == null) {
            return PREFIX + relation.name();
        }
        return PREFIX + relation.name() + RELATION_SUFFIX + damageCause.name();
    }

    /**
     * Whether this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return damageCause != null;
    }

    /**
     * Whether this stat contains this statName
     *
     * @param statName
     * @return
     */
    @Override
    public boolean containsStat(String statName) {
        return getStatName().startsWith(statName);
    }

    public enum Relation {
        RECEIVED,
        DEALT
    }

}
