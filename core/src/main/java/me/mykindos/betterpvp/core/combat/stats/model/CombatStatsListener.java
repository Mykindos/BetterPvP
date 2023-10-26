package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public abstract class CombatStatsListener<T extends CombatData> implements Listener {

    private final DamageLogManager logManager;
    private final Leaderboard<UUID, CombatData> leaderboard;

    protected CombatStatsListener(DamageLogManager logManager, Leaderboard<UUID, CombatData> leaderboard) {
        this.logManager = logManager;
        this.leaderboard = leaderboard;
    }

    protected abstract void onSave();

    protected abstract StatsRepository<?> getAssignedRepository(Player player);

    protected abstract CompletableFuture<T> getCombatData(Player player);

    @UpdateEvent(delay = 60_000 * 5) // save async every 5 minutes
    public void onUpdate() {
        onSave();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        final DamageLog lastDamager = logManager.getLastDamager(event.getPlayer());
        if (!(lastDamager.getDamager() instanceof Player killer)) {
            return;
        }

        // Get involved players
        final Player victim = event.getPlayer();
        final ConcurrentLinkedDeque<DamageLog> assistLog = logManager.getObject(victim.getUniqueId()).orElse(new ConcurrentLinkedDeque<>());
        final Map<Player, Contribution> contributions = getContributions(assistLog);

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
            victimData.killed(killerData, contributorData);

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
            log.error("Failed to save combat data for " + event.getPlayer(), throwable);
            return null;
        });
    }

    private Map<Player, Contribution> getContributions(ConcurrentLinkedDeque<DamageLog> damageLog) {
        // Get total damage done by players
        final Map<Player, Float> damages = new HashMap<>();
        for (DamageLog log : damageLog) {
            final LivingEntity damager = log.getDamager();
            if (damager instanceof Player attacker) {
                final float damage = (float) log.getDamage();
                damages.compute(attacker, (key, value) -> value == null ? damage : value + damage);
            }
        }

        // Calculate contributions
        final float totalDamage = (float) damageLog.stream().mapToDouble(DamageLog::getDamage).sum();
        final Map<Player, Contribution> contributions = new HashMap<>();
        for (Map.Entry<Player, Float> entry : damages.entrySet()) {
            final Player attacker = entry.getKey();
            final float damage = entry.getValue();
            final float percent = damage / totalDamage; // 0.0 - 1.0
            contributions.put(attacker, new Contribution(attacker.getUniqueId(), damage, percent));
        }

        return contributions;
    }

}
