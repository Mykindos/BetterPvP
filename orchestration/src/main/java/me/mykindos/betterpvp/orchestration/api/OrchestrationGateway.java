package me.mykindos.betterpvp.orchestration.api;

import me.mykindos.betterpvp.orchestration.model.AdmissionConfirmation;
import me.mykindos.betterpvp.orchestration.model.AdmissionDecision;
import me.mykindos.betterpvp.orchestration.model.DepartureNotification;
import me.mykindos.betterpvp.orchestration.model.JoinIntent;
import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.ServerCapacitySnapshot;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OrchestrationGateway {

    CompletableFuture<AdmissionDecision> requestJoin(JoinIntent intent);

    CompletableFuture<Void> leaveQueue(UUID playerUuid);

    CompletableFuture<Void> updateCapacity(ServerCapacitySnapshot snapshot);

    CompletableFuture<Void> confirmArrival(AdmissionConfirmation confirmation);

    CompletableFuture<Void> notifyDeparture(DepartureNotification notification);

    CompletableFuture<Void> upsertPlayerRank(PlayerRankSnapshot snapshot);

    CompletableFuture<Void> removePlayerRank(UUID playerUuid);

    CompletableFuture<Optional<PlayerRankSnapshot>> getPlayerRank(UUID playerUuid);

    CompletableFuture<Optional<QueueStatusUpdate>> getPlayerQueueStatus(UUID playerUuid);

    CompletableFuture<QueueSnapshot> getQueueSnapshot(QueueTarget target);

    CompletableFuture<Void> setQueueState(QueueTarget target, QueueState state);

    CompletableFuture<Boolean> removeQueuedPlayer(UUID playerUuid);

    CompletableFuture<Boolean> admitQueuedPlayer(UUID playerUuid);
}
