package me.mykindos.betterpvp.core.client.stats.impl;

import me.mykindos.betterpvp.core.client.stats.StatContainer;

public interface IStat {
    /**
     * Get the stat represented by this object from the statContainer
     * @param statContainer
     * @param period
     * @return
     */
    Double getStat(StatContainer statContainer, String period);

    String getStatName();

    /**
     * Whether this stat is directly savable to the database
     * @return {@code true} if it is, {@code false} otherwise
     */
    boolean isSavable();

    /**
     * Whether this stat contains this statName
     * @param statName
     * @return
     */
    boolean containsStat(String statName);
}
