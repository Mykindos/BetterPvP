package me.mykindos.betterpvp.core.client.stats.impl;

import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;

/**
 * A class used to tag stats that wrap other stats
 */
public interface IWrapperStat extends IStat {
    /**
     * Get the stat wrapped by this stat
     * @return
     */
    IStat getWrappedStat();

    /**
     * What type of stat this is, a LONG (default), DOUBLE, OR DURATION
     *
     * @return the type of stat
     */
    @Override
    default StatValueType getStatValueType() {
        return getWrappedStat().getStatValueType();
    }
}
