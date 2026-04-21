package me.mykindos.betterpvp.orchestration.service;

import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.QueueTargetType;
import me.mykindos.betterpvp.orchestration.model.ServerCapacitySnapshot;
import me.mykindos.betterpvp.orchestration.policy.QueuePriorityPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryOrchestrationGatewayTest {

    @Test
    void staleRankSnapshotsAreNotCountedInRegularOnline() {
        final InMemoryOrchestrationGateway gateway = createGateway();
        final UUID stalePlayer = UUID.randomUUID();
        final UUID freshPlayer = UUID.randomUUID();
        final Instant now = Instant.now();

        gateway.upsertPlayerRank(new PlayerRankSnapshot(
                stalePlayer,
                "stale",
                "PLAYER",
                "hub-1",
                now.minusSeconds(20)
        )).join();
        gateway.upsertPlayerRank(new PlayerRankSnapshot(
                freshPlayer,
                "fresh",
                "PLAYER",
                "hub-1",
                now
        )).join();

        gateway.updateCapacity(new ServerCapacitySnapshot(
                "hub-1",
                QueueState.OPEN,
                100,
                10,
                0,
                0,
                now
        )).join();

        final int regularOnline = gateway.getQueueSnapshot(new QueueTarget("hub:hub-1", QueueTargetType.HUB, "hub-1")).join().regularOnline();
        assertEquals(1, regularOnline);
    }

    private InMemoryOrchestrationGateway createGateway() {
        final OrchestrationServiceConfig config = new OrchestrationServiceConfig(
                "0.0.0.0",
                8085,
                10L,
                0L,
                30L,
                5,
                "PLAYER",
                Map.of("PLAYER", new OrchestrationServiceConfig.RankPolicy(0, false)),
                List.of()
        );

        return new InMemoryOrchestrationGateway(
                new QueuePriorityPolicy(30L, 5),
                10L,
                0L,
                new PlayerQueuePolicyResolver(config),
                new ManagedTargetResolver(List.of(new ManagedTargetResolver.ManagedTargetRule(
                        Pattern.compile("^hub-\\d+$"),
                        QueueTargetType.HUB,
                        null
                )))
        );
    }
}
