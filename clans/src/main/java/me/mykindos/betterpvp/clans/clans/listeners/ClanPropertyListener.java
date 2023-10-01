package me.mykindos.betterpvp.clans.clans.listeners;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanPropertyUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;

import javax.inject.Inject;

@BPvPListener
public class ClanPropertyListener extends ClanListener{

    @Inject
    public ClanPropertyListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @EventHandler
    public void onSettingsUpdated(ClanPropertyUpdateEvent event) {
        clanManager.getRepository().saveProperty(event.getClan(), event.getProperty(), event.getValue());
    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates(){
        clanManager.getRepository().processPropertyUpdates(true);
    }

}
