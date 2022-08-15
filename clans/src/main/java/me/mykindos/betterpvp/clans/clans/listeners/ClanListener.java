package me.mykindos.betterpvp.clans.clans.listeners;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import org.bukkit.event.Listener;

public abstract class ClanListener implements Listener {

    protected final ClanManager clanManager;
    protected final GamerManager gamerManager;

    public ClanListener(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
    }
}
