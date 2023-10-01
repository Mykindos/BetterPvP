package me.mykindos.betterpvp.clans.clans.tips;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.tips.TipListener;
import me.mykindos.betterpvp.core.tips.TipManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@BPvPListener
public class ClanTipListener extends TipListener {

    public final ClanManager clanManager;
    @Inject
    ClanTipListener(Core core, ClanManager clanManager, GamerManager gamerManager, TipManager tipManager) {
        super(core, gamerManager, tipManager);
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.LOW)


}
