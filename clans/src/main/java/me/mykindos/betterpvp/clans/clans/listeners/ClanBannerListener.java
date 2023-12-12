package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanBannerUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@BPvPListener
public class ClanBannerListener extends ClanListener {

    @Inject
    public ClanBannerListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSave(ClanBannerUpdateEvent event) {
        clanManager.getRepository().updateClanBanner(event.getClan());
    }
}
