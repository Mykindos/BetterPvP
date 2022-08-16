package me.mykindos.betterpvp.champions.properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


@Singleton
@BPvPListener
public class ChampionsPropertyListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public ChampionsPropertyListener(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onPropertyUpdated(ChampionsPropertyUpdateEvent event) {
        gamerManager.getGamerRepository().saveProperty(event.getGamer(), event.getProperty(), event.getValue());
    }
}
