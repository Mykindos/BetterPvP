package me.mykindos.betterpvp.champions.stats.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.client.stats.events.GetDefaultTrackedStatsEvent;
import me.mykindos.betterpvp.core.client.stats.impl.ChampionsSkillStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class SkillListener implements Listener {
    private final ChampionsSkillManager skillManager;

    @Inject
    public SkillListener(ChampionsSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void skillStats(GetDefaultTrackedStatsEvent event) {
        skillManager.getObjects().values().forEach(skill -> {
            final ChampionsSkillStat useStat = ChampionsSkillStat.builder()
                    .action(ChampionsSkillStat.Action.USE)
                    .skill(skill)
                    .build();
            final ChampionsSkillStat equipStat = ChampionsSkillStat.builder()
                    .action(ChampionsSkillStat.Action.EQUIP)
                    .skill(skill)
                    .build();
            event.addStats(useStat, equipStat);
        });
    }
}
