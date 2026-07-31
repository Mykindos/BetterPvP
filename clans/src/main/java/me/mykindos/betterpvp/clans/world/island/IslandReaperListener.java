package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Releases discovery island instances that have sat empty for longer than the configured grace period. An instance
 * with occupants is never touched, no matter how long it has been alive.
 */
@CustomLog
@BPvPListener
@Singleton
public class IslandReaperListener implements Listener {

    private final IslandInstanceManager instanceManager;

    @Inject
    @Config(path = "islands.reaper.grace-period-ms", defaultValue = "120000")
    private long gracePeriodMs;

    @Inject
    public IslandReaperListener(@NotNull IslandInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @UpdateEvent(delay = 10_000L)
    public void reapEmptyInstances() {
        final long now = System.currentTimeMillis();

        for (IslandInstance instance : instanceManager.all()) {
            if (!shouldReap(instance, now, gracePeriodMs)) {
                continue;
            }

            log.info("Reaping empty island instance {} ({}) after grace period", instance.getId(), instance.getTemplate().getKey()).submit();
            instanceManager.release(instance.getId());
        }
    }

    /**
     * @return whether {@code instance} has sat empty in {@link IslandInstanceState#READY} for at least
     * {@code gracePeriodMs}, and should therefore be released
     */
    static boolean shouldReap(@NotNull IslandInstance instance, long now, long gracePeriodMs) {
        return instance.isEmpty()
                && instance.getState() == IslandInstanceState.READY
                && now - instance.getLastVacatedAt() >= gracePeriodMs;
    }

}
