package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.log.DamageLogManager;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.CombatStatsListener;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import org.bukkit.entity.Player;

import java.util.Map;

@BPvPListener
@Singleton
public class GlobalCombatListener extends CombatStatsListener<GlobalCombatData> {

    private final GlobalCombatStatsRepository repository;

    private final GlobalCombatLeaderboard leaderboard;

    @Inject
    protected GlobalCombatListener(DamageLogManager logManager, GlobalCombatStatsRepository repository, GlobalCombatLeaderboard leaderboard) {
        super(logManager);
        this.repository = repository;
        this.leaderboard = leaderboard;
    }

    @Override
    protected void onSave() {
        repository.saveAll(true);
    }

    @Override
    protected StatsRepository<GlobalCombatData> getAssignedRepository(Player player) {
        return repository;
    }

    @Override
    protected void updateLeaderboard(Player victim, GlobalCombatData victimData, Player killer, CombatData killerData, Map<Player, Contribution> contributions, Map<CombatData, Contribution> contributorData) {
        final Map<SortType, Integer> killerUpdate = leaderboard.compute(killer.getUniqueId(), killerData);
        leaderboard.compute(victim.getUniqueId(), victimData);
        contributorData.remove(killerData);
        for (CombatData combatData : contributorData.keySet()) {
            leaderboard.compute(combatData.getHolder(), combatData);
        }

        // Only announce for killer since he's the one that gained rating
        leaderboard.attemptAnnounce(killer, killerUpdate);
    }
}
