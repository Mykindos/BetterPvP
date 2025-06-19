package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.events.GetDefaultTrackedStatsEvent;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.MinecraftStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

@BPvPListener
@Singleton
@CustomLog
public class SkillListener implements Listener {

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
}
