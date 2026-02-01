package me.mykindos.betterpvp.core.client.stats.impl;

/**
 * A class used to tag stats that wrap other stats
 */
public interface IWrapperStat extends IStat {
    /**
     * Get the stat wrapped by this stat
     * @return
     */
    IStat getWrappedStat();
}
