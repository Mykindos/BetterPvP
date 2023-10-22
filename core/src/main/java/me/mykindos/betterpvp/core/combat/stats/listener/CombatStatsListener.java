package me.mykindos.betterpvp.core.combat.stats.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.log.DamageLog;
import me.mykindos.betterpvp.core.combat.log.DamageLogManager;
import me.mykindos.betterpvp.core.combat.stats.CombatData;
import me.mykindos.betterpvp.core.combat.stats.CombatStatsRepository;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

@BPvPListener
@Slf4j
public class CombatStatsListener implements Listener {

    @Inject
    private CombatStatsRepository statsRepository;

    @Inject
    private DamageLogManager logManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        final DamageLog lastDamager = logManager.getLastDamager(event.getPlayer());
        if (!(lastDamager.getDamager() instanceof Player killer)) {
            return; // Return because it wasn't combat related.
        }

        // Get involved players
        final Player victim = event.getPlayer();
        final ConcurrentLinkedDeque<DamageLog> assistLog = logManager.getObject(victim.getUniqueId()).orElse(new ConcurrentLinkedDeque<>());
        assistLog.remove(lastDamager);
        final List<Player> assists = assistLog.stream()
                .filter(log -> log.getDamager() instanceof Player && log.getDamager() != killer) // only players who arent the killer
                .map(log -> (Player) log.getDamager()).toList();

        // Create atomic references
        final AtomicReference<CombatData> victimData = new AtomicReference<>();
        final AtomicReference<CombatData> killerData = new AtomicReference<>();
        final AtomicReferenceArray<CombatData> assistData = new AtomicReferenceArray<>(assists.size());

        // Load victim async
        statsRepository.getDataAsync(event.getPlayer()).whenComplete((victimDataComplete, throwable) -> {
            victimData.set(victimDataComplete);
            // Load killer and assists async
            statsRepository.getDataAsync(killer).whenCompleteAsync((killerDataComplete, throwable1) -> {
                killerData.set(killerDataComplete);
                for (int i = 0; i < assists.size(); i++) {
                    // We can join because we are already on an async thread
                    CombatData assistDataComplete = statsRepository.getDataAsync(assists.get(i)).join();
                    assistData.set(i, assistDataComplete);
                }
            }).thenRun(() -> {
                // Main thread
                List<CombatData> assisters = new ArrayList<>();
                for (int i = 0; i < assistData.length(); i++) {
                    assisters.add(assistData.get(i));
                }
                killerData.get().killed(victimData.get(), assisters);
            });
        }).exceptionally(throwable -> {
            log.error("Failed to save combat data for " + event.getPlayer(), throwable);
            return null;
        });
    }

}
