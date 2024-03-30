package me.mykindos.betterpvp.clans.combat;

import me.mykindos.betterpvp.clans.logging.ClanLogger;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@BPvPListener
public class ClansKillListener {
    private final ClanLogger clanLogger;


    public ClansKillListener(ClanLogger clanLogger) {
        this.clanLogger = clanLogger;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClanKill(KillContributionEvent event) {
        clanLogger.addClanKill(event.getKillId(), event.getKiller(), event.getVictim());
    }
}
