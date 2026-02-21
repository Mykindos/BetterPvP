package me.mykindos.betterpvp.core.client.stats.impl;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;


public interface IBuildableStat extends IStat {
    /**
     * Copies the stat represented by this statName into this object
     *
     * @param statType the statname
     * @param data
     * @return this stat
     * @throws IllegalArgumentException if this statName does not represent this stat
     */
    @NotNull
    @Contract(value = "_, _ -> this", mutates = "this")
    IBuildableStat copyFromStatData(@NotNull String statType, JSONObject data);
}
