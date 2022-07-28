package me.mykindos.betterpvp.clans.gamer.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class GamerListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public GamerListener(GamerManager gamerManager){
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent e) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(e.getClient().getUuid());
        if(gamerOptional.isEmpty()){
            Gamer gamer = new Gamer(e.getClient(), e.getClient().getUuid());
            gamerManager.addObject(e.getClient().getUuid(), gamer);
            gamerManager.getGamerRepository().save(gamer);
        }
    }
}
