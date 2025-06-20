package me.mykindos.betterpvp.core.client.stats.impl;

import me.mykindos.betterpvp.core.utilities.model.IStringNameDescription;

public interface IClientStat extends IStringNameDescription, IStat {
    String name();
}
