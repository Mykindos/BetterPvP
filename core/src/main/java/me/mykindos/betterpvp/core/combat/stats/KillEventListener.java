package me.mykindos.betterpvp.core.combat.stats;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@BPvPListener
public class KillEventListener implements Listener {

    @Inject
    private DamageLogManager logManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        final DamageLog lastDamager = logManager.getLastDamager(event.getPlayer());
        if (lastDamager == null || !(lastDamager.getDamager() instanceof Player killer)) {
            return;
        }

        // Get involved players
        final Player victim = event.getPlayer();
        final ConcurrentLinkedDeque<DamageLog> assistLog = logManager.getObject(victim.getUniqueId()).orElse(new ConcurrentLinkedDeque<>());
        final Map<Player, Contribution> contributions = getContributions(assistLog);
        final KillContributionEvent ke = new KillContributionEvent(victim, killer, Collections.unmodifiableMap(contributions));
        UtilServer.callEvent(ke);
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
