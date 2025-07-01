package me.mykindos.betterpvp.core.client.stats.impl;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public interface IBuildableStat extends IStat {
    /**
     * Copies the stat represented by this statName into this object
     * @param statName the statname
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @Contract(value = "_ -> this", mutates = "this")
    IBuildableStat copyFromStatname(@NotNull String statName);
    String getPrefix();
}
