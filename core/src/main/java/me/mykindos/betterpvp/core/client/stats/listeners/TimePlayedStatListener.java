package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;

@Singleton
@BPvPListener
public class TimePlayedStatListener extends TimedStatListener {
    @Inject
    protected TimePlayedStatListener(ClientManager clientManager) {
        super(clientManager);
    }

    @Override
    public void onUpdate(Client client, long deltaTime) {
        client.getStatContainer().incrementStat(ClientStat.TIME_PLAYED, deltaTime);
    }
}
