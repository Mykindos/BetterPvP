package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.logging.ClanLogger;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ClansKillListener implements Listener {
    private final ClanLogger clanLogger;

    @Inject
    public ClansKillListener(ClanLogger clanLogger) {
        this.clanLogger = clanLogger;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClanKill(KillContributionEvent event) {
        clanLogger.addClanKill(event.getKillId(), event.getKiller(), event.getVictim());
    }
}
