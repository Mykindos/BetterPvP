package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.log.DamageLog;
import me.mykindos.betterpvp.core.combat.log.DamageLogManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public abstract class CombatStatsListener<T extends CombatData> implements Listener {

    private final DamageLogManager logManager;

    protected CombatStatsListener(DamageLogManager logManager) {
        this.logManager = logManager;
    }

    protected abstract void onSave();

    protected abstract StatsRepository<T> getAssignedRepository(Player player);

    protected abstract void updateLeaderboard(Player victim, T victimData, Player killer, CombatData killerData, Map<Player, Contribution> contributions, Map<CombatData, Contribution> contributorData) ;

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
        final StatsRepository<T> statsRepository = getAssignedRepository(victim);
        statsRepository.getDataAsync(victim).whenCompleteAsync((victimData, throwable) -> {
            // Load contributors
            CombatData killerData = null;
            Map<CombatData, Contribution> contributorData = new HashMap<>();
            for (Map.Entry<Player, Contribution> contribution : contributions.entrySet()) {
                final Player contributor = contribution.getKey();
                // We can join because we are already on an async thread
                CombatData otherData = statsRepository.getDataAsync(contributor).join();
                if (contributor.getUniqueId() == killer.getUniqueId()) {
                    killerData = otherData;
                }
                contributorData.put(otherData, contribution.getValue());
            }

            if (killerData == null) {
                log.error("Failed to find killer combat data for " + killer + ". Maybe they killed the player but dealt no damage?");
                return;
            }

            // Main thread
            victimData.killed(killerData, contributorData);

            // Save everybody's stats
            statsRepository.saveAsync(victim);
            contributions.keySet().forEach(statsRepository::saveAsync);

            // Update leaderboard
            updateLeaderboard(victim, victimData, killer, killerData, contributions, contributorData);
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
