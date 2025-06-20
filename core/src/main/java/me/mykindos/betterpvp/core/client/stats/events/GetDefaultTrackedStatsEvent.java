package me.mykindos.betterpvp.core.client.stats.events;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GetDefaultTrackedStatsEvent extends CustomEvent {
    @Getter
    private final List<IStat> defaultTrackedStats = new ArrayList<>();

    public void addStat(IStat iStat) {
        defaultTrackedStats.add(iStat);
    }

    public void addStats(Collection<IStat> iStats) {
        defaultTrackedStats.addAll(iStats);
    }

    public void addStats(IStat... iStats) {
        defaultTrackedStats.addAll(List.of(iStats));
    }
}
