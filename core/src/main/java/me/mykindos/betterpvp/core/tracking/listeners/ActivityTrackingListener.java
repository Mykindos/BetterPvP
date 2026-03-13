package me.mykindos.betterpvp.core.tracking.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.tracking.ActivitySnapshot;
import me.mykindos.betterpvp.core.tracking.PlayerActivityService;
import me.mykindos.betterpvp.core.tracking.model.GridKey;
import me.mykindos.betterpvp.core.tracking.repository.ActivitySnapshotRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;

@BPvPListener
public class ActivityTrackingListener implements Listener {

    @Inject
    @Config(path = "activity.min-damage-threshold", defaultValue = "5.0")
    private double minDamageThreshold;

    private final Core core;
    private final PlayerActivityService activityService;
    private final ActivitySnapshotRepository repository;
    private final DamageLogManager damageLogManager;

    @Inject
    public ActivityTrackingListener(Core core, PlayerActivityService activityService,
                                    ActivitySnapshotRepository repository, DamageLogManager damageLogManager) {
        this.core = core;
        this.activityService = activityService;
        this.repository = repository;
        this.damageLogManager = damageLogManager;
    }

    /**
     * Samples every online player's position every 5 seconds and adds presence heat.
     * Spectators and players outside the main world are skipped.
     */
    @UpdateEvent(delay = 5_000)
    public void samplePositions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (!player.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) continue;
            activityService.recordPresence(player.getLocation());
        }
    }

    /**
     * Every 30 seconds: decay all heat values, prune dead cells, then refresh the
     * immutable snapshot (which includes live player counts at the moment of snapshot).
     */
    @UpdateEvent(delay = 30_000)
    public void decayAndSnapshot() {
        Map<GridKey, Integer> liveCounts = buildLiveCounts();
        activityService.decay();
        activityService.refreshSnapshot(liveCounts);
    }

    /**
     * Every 5 minutes: take the current snapshot and persist it to the database on
     * an async thread. The snapshot is immutable so it is safe to hand off.
     */
    @UpdateEvent(delay = 300_000)
    public void persistSnapshot() {
        ActivitySnapshot snapshot = activityService.getSnapshot();
        if (snapshot == null || snapshot.entries().isEmpty()) return;
        UtilServer.runTaskAsync(core, () -> repository.persist(snapshot));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (event.getModifiedDamage() < minDamageThreshold) return;
        if (!damagee.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) return;
        activityService.recordCombat(damagee.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!victim.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) return;
        DamageLog lastDamage = damageLogManager.getLastDamager(victim);
        if (lastDamage == null || !(lastDamage.getDamager() instanceof Player)) return;
        activityService.recordKill(victim.getLocation());
    }

    private Map<GridKey, Integer> buildLiveCounts() {
        Map<GridKey, Integer> counts = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            if (!player.getWorld().getName().equals(BPvPWorld.MAIN_WORLD_NAME)) continue;
            counts.merge(GridKey.of(player.getLocation()), 1, Integer::sum);
        }
        return counts;
    }

}
