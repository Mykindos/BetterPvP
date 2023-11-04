package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.stats.model.CombatStatsListener;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@BPvPListener
@Singleton
public class GlobalCombatListener extends CombatStatsListener<GlobalCombatData> {

    private final GlobalCombatStatsRepository repository;

    @Inject
    protected GlobalCombatListener(DamageLogManager logManager, GlobalCombatStatsRepository repository, GlobalCombatLeaderboard leaderboard) {
        super(logManager, leaderboard);
        this.repository = repository;
    }

    @UpdateEvent(delay = 60_000 * 5) // save async every 5 minutes
    public void onUpdate() {
        repository.saveAll(true);
    }

    @Override
    protected StatsRepository<GlobalCombatData> getAssignedRepository(Player player) {
        return repository;
    }

    @Override
    protected CompletableFuture<GlobalCombatData> getCombatData(Player player) {
        return getAssignedRepository(player).getDataAsync(player.getUniqueId());
    }
}
