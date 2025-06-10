package me.mykindos.betterpvp.core.client.stats.formatter.stat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.stats.ClientStat;
import me.mykindos.betterpvp.core.client.stats.formatter.ClientStatFormatter;

@Singleton
public class DeathFormatter extends ClientStatFormatter {
    @Inject
    public DeathFormatter() {
        super(ClientStat.DEATHS);
    }
}
