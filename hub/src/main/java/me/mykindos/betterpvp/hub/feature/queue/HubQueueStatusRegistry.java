package me.mykindos.betterpvp.hub.feature.queue;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class HubQueueStatusRegistry {

    private final ConcurrentMap<UUID, QueueStatusUpdate> statuses = new ConcurrentHashMap<>();

    public void applyUpdate(QueueStatusUpdate update) {
        if (!isForCurrentServer(update)) {
            return;
        }

        if (!update.active()) {
            statuses.remove(update.playerUuid());
            return;
        }

        statuses.compute(update.playerUuid(), (uuid, existing) -> {
            if (existing == null || update.displayVersion() >= existing.displayVersion()) {
                return update;
            }
            return existing;
        });
    }

    public Optional<QueueStatusUpdate> getStatus(UUID playerUuid) {
        return Optional.ofNullable(statuses.get(playerUuid));
    }

    public void clear(UUID playerUuid) {
        statuses.remove(playerUuid);
    }

    public Collection<QueueStatusUpdate> getOrderedStatuses() {
        return statuses.values().stream()
                .sorted(Comparator
                        .comparing((QueueStatusUpdate update) -> update.queuedTarget().serverName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(QueueStatusUpdate::position)
                        .thenComparing(QueueStatusUpdate::playerUuid))
                .toList();
    }

    private boolean isForCurrentServer(QueueStatusUpdate update) {
        return Core.getCurrentRealm().getServer().getName().equalsIgnoreCase(update.currentServer());
    }
}
