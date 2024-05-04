package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
public abstract class CombatStatsListener<T extends CombatData> implements Listener {

    private final DamageLogManager logManager;
    private final Leaderboard<UUID, CombatData> leaderboard;

    protected CombatStatsListener(DamageLogManager logManager, Leaderboard<UUID, CombatData> leaderboard) {
        this.logManager = logManager;
        this.leaderboard = leaderboard;
    }

    protected abstract StatsRepository<?> getAssignedRepository(Player player);

    protected abstract CompletableFuture<T> getCombatData(Player player);

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(KillContributionEvent event) {
        final Player victim = event.getVictim();
        final Player killer = event.getKiller();
        final Map<Player, Contribution> contributions = event.getContributions();
        // Load victim async
        getCombatData(victim).whenCompleteAsync((victimData, throwable) -> {
            // Load contributors
            CombatData killerData = null;
            Map<CombatData, Contribution> contributorData = new HashMap<>();
            Map<Player, StatsRepository<?>> statsRepositories = new HashMap<>();
            for (Map.Entry<Player, Contribution> contribution : contributions.entrySet()) {
                final Player contributor = contribution.getKey();
                // We can join because we are already on an async thread
                CombatData otherData = getCombatData(contributor).join();
                if (contributor.getUniqueId() == killer.getUniqueId()) {
                    killerData = otherData;
                }
                contributorData.put(otherData, contribution.getValue());
                final StatsRepository<?> assignedRepository = getAssignedRepository(contributor);
                statsRepositories.put(contributor, assignedRepository);
            }

            if (killerData == null) {
                log.error("Failed to find killer combat data for " + killer + ". Maybe they killed the player but dealt no damage?");
                return;
            }

            // Main thread
            victimData.killed(event.getKillId(), killerData, contributorData);

            // Save everybody's stats
            getAssignedRepository(victim).saveAsync(victim);
            statsRepositories.forEach((player, repository) -> repository.saveAsync(player));

            // Update leaderboard
            final Map<SearchOptions, Integer> killerUpdate = leaderboard.compute(killer.getUniqueId(), killerData);
            leaderboard.compute(victim.getUniqueId(), victimData);
            contributorData.remove(killerData);
            for (CombatData combatData : contributorData.keySet()) {
                leaderboard.compute(combatData.getHolder(), combatData);
            }

            // Only announce for killer since he's the one that gained rating
            leaderboard.attemptAnnounce(killer, killerUpdate);
        }).exceptionally(throwable -> {
            log.error("Failed to save combat data for " + victim.getName(), throwable).submit();
            return null;
        });
    }

}
