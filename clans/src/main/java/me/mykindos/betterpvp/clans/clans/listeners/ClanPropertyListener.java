package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;

@BPvPListener
public class ClanPropertyListener extends ClanListener{

    @Inject
    public ClanPropertyListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
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
