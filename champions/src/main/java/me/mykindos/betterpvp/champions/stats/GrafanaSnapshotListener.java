package me.mykindos.betterpvp.champions.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.stats.repository.GrafanaSnapshotRepository;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

/**
 * Periodically snapshots Champions analytics data into Grafana tables.
 *
 * <p>Uses the standard {@link UpdateEvent} scheduling mechanism so the Champions plugin
 * owns and drives its own analytics cadence — no external scripts required.</p>
 *
 * <p>Snapshots run once per hour on an async thread to avoid blocking the main thread.</p>
 */
@BPvPListener
@Singleton
public class GrafanaSnapshotListener implements Listener {

    private final GrafanaSnapshotRepository snapshotRepository;

    @Inject
    public GrafanaSnapshotListener(GrafanaSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Takes a full Grafana snapshot (role matchups, role playtime, skill KDR) once per hour.
     */
    @UpdateEvent(delay = 3_600_000L, isAsync = true)
    public void takeHourlySnapshot() {
        snapshotRepository.takeSnapshot(Core.getCurrentRealm().getId());
    }
}

