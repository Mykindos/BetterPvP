package me.mykindos.betterpvp.core.client.stats.impl.utility;

import me.mykindos.betterpvp.core.client.stats.impl.IStat;

public enum StatValueType {
    /**
     * Default value, what stats are natively stored as
     */
    LONG,
    /**
     * A double stat. Stored as a long with {@link IStat#FP_MODIFIER} applied
     */
    DOUBLE,
    /**
     * A duration stat, stored as milliseconds
     */
    DURATION
}
