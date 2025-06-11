package me.mykindos.betterpvp.core.client.stats;

import me.mykindos.betterpvp.core.utilities.model.IStringNameDescription;

public interface IClientStat extends IStringNameDescription, IStat {
    String name();
}
