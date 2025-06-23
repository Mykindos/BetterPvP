package me.mykindos.betterpvp.core.client.stats.formatter;

import me.mykindos.betterpvp.core.client.stats.impl.IClientStat;


public class GenericClientStatFormatter extends ClientStatFormatter {
    public GenericClientStatFormatter(IClientStat iClientStat) {
        super(iClientStat);
    }

    @Override
    public String getStatType() {
        return null;
    }
}
