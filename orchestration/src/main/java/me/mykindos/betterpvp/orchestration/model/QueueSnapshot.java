package me.mykindos.betterpvp.orchestration.model;

import java.util.List;
import java.util.Objects;

public record QueueSnapshot(
        QueueTarget target,
        QueueState state,
        int queueSize,
        int softCapacity,
        int regularOnline,
        int bypassOnline,
        int reservedRegularSlots,
        List<QueueEntryView> entries
) {

    public QueueSnapshot {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(state, "state");
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}
