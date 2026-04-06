package me.mykindos.betterpvp.orchestration.service;

import me.mykindos.betterpvp.orchestration.model.JoinIntent;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.QueueTicket;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

final class ManagedQueueEntry {

    private final String playerName;
    private final QueueTicket ticket;
    private final AtomicLong displayVersion = new AtomicLong();

    private ManagedQueueEntry(String playerName, QueueTicket ticket) {
        this.playerName = playerName;
        this.ticket = ticket;
    }

    public static ManagedQueueEntry from(JoinIntent intent, QueueTarget target, int basePriority) {
        return new ManagedQueueEntry(
                intent.playerName(),
                new QueueTicket(
                        UUID.randomUUID().toString(),
                        intent.playerUuid(),
                        intent.currentServer(),
                        target,
                        basePriority,
                        intent.requestedAt(),
                        Instant.now(),
                        QueueState.OPEN
                )
        );
    }

    public String playerName() {
        return playerName;
    }

    public QueueTicket ticket() {
        return ticket;
    }

    public long nextDisplayVersion() {
        return displayVersion.incrementAndGet();
    }
}
