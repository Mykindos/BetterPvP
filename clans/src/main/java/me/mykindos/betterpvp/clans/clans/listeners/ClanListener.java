package me.mykindos.betterpvp.clans.clans.listeners;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.event.Listener;

public abstract class ClanListener implements Listener {

    protected final ClanManager clanManager;
    protected final ClientManager clientManager;

    public ClanListener(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }
}
