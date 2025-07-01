package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.events.GetDefaultTrackedStatsEvent;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.DamageStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.MinecraftStat;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utilitiy.Type;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@BPvPListener
@Singleton
@CustomLog
public class StatListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    public StatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler
    public void standardStats(GetDefaultTrackedStatsEvent event) {
        event.addStats(ClientStat.values());
    }

    @EventHandler
    public void statisticsStats(GetDefaultTrackedStatsEvent event) {
        List<IStat> stats = Arrays.stream(Statistic.values()).map(statistic ->
            MinecraftStat.builder()
                    .statistic(statistic)
                .build()
        ).map(IStat.class::cast)
                .toList();

        event.addStats(stats);
    }

    @EventHandler
    public void damageStats(GetDefaultTrackedStatsEvent event) {
        event.addStat(DamageStat.builder().relation(Relation.DEALT).type(Type.AMOUNT).build());
        event.addStat(DamageStat.builder().relation(Relation.RECEIVED).type(Type.AMOUNT).build());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(ClientQuitEvent event) {
        log.error("process stats on quit").submit();
        clientManager.getSqlLayer().processStatUpdates(Set.of(event.getClient()), StatContainer.PERIOD);
    }
}
