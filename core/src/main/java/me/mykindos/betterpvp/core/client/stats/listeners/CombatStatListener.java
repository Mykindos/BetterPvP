package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;

@BPvPListener
@Singleton
@CustomLog
public class CombatStatListener implements Listener {
    private final ClientManager clientManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public CombatStatListener(ClientManager clientManager, DamageLogManager damageLogManager) {
        this.clientManager = clientManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void killContributionListener(KillContributionEvent event) {
        final Player killer = event.getKiller();
        final Player victim = event.getVictim();
        final Set<Player> assisters = event.getContributions().keySet();
        final ClientStat killerStat = ClientStat.PLAYER_KILLS;
        final ClientStat victimStat = ClientStat.PLAYER_DEATHS;
        final ClientStat assistStat = ClientStat.PLAYER_KILL_ASSISTS;

        clientManager.incrementStat(killer, killerStat, 1);
        clientManager.incrementStat(victim, victimStat, 1);
        assisters.forEach(assister -> clientManager.incrementStat(assister, assistStat, 1));
    }
}
