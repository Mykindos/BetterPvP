package me.mykindos.betterpvp.orchestration.model;

import java.util.Objects;

public record QueueTarget(
        String targetId,
        QueueTargetType targetType,
        String serverName
) {

    public QueueTarget {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(serverName, "serverName");
    }
}
