package me.mykindos.betterpvp.core.combat.listeners;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.DamageStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Relation;
import me.mykindos.betterpvp.core.client.stats.impl.utility.Type;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles damage statistics tracking and logging
 */
@CustomLog
@BPvPListener
public class CombatStatisticsListener implements Listener {
    
    private final ClientManager clientManager;
    private final DamageLogManager damageLogManager;
    
    @Inject
    public CombatStatisticsListener(ClientManager clientManager, DamageLogManager damageLogManager) {
        this.clientManager = clientManager;
        this.damageLogManager = damageLogManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onDamageStatistics(DamageEvent event) {
        // Skip if no damage was dealt
        if (event.getDamage() <= 0) {
            return;
        }
        
        // Process damage taken statistics
        processPlayerDamageTaken(event);
        
        // Process damage dealt statistics
        processPlayerDamageDealt(event);
        
        // Record damage log
        recordDamageLog(event);
        
        log.debug("Recorded statistics for damage event: {} damage", event.getDamage()).submit();
    }
    
    /**
     * Processes damage taken statistics for players
     */
    private void processPlayerDamageTaken(DamageEvent event) {
        if (!(event.getDamagee() instanceof Player damagee)) {
            return;
        }
        
        final DamageStat.DamageStatBuilder builder = DamageStat.builder()
                .relation(Relation.RECEIVED)
                .damageCause(event.getCause());

        clientManager.incrementStat(damagee, builder.type(Type.COUNT).build(), 1);
        clientManager.incrementStat(damagee, builder.type(Type.AMOUNT).build(), (long) (event.getDamage() * IStat.FP_MODIFIER));
    }
    
    /**
     * Processes damage dealt statistics for players
     */
    private void processPlayerDamageDealt(DamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        final DamageStat.DamageStatBuilder builder = DamageStat.builder()
                .relation(Relation.DEALT)
                .damageCause(event.getCause());

        clientManager.incrementStat(damager, builder.type(Type.COUNT).build(), 1);
        clientManager.incrementStat(damager, builder.type(Type.AMOUNT).build(), (long) (event.getDamage() * IStat.FP_MODIFIER));
    }
    
    /**
     * Records the damage event in the damage log
     */
    private void recordDamageLog(DamageEvent event) {
        DamageLog damageLog = new DamageLog(
            event.getDamager(),
            event.getCause(),
            event.getDamage(),
            event.getReasons()
        );
        
        damageLogManager.add(event.getDamagee(), damageLog);
        
        log.debug("Added damage log entry: {} -> {} ({} damage)", 
                 event.getDamager() != null ? event.getDamager().getName() : "Environment",
                 event.getDamagee().getName(), event.getDamage()).submit();
    }
    
    /**
     * Checks if the damage event is fall damage
     */
    private boolean isFallDamage(DamageEvent event) {
        return event.getCause() instanceof VanillaDamageCause cause && cause.getVanillaCause() == EntityDamageEvent.DamageCause.FALL;
    }
}
