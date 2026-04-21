package me.mykindos.betterpvp.orchestration.service;

import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.model.AdmissionConfirmation;
import me.mykindos.betterpvp.orchestration.model.AdmissionDecision;
import me.mykindos.betterpvp.orchestration.model.DepartureNotification;
import me.mykindos.betterpvp.orchestration.model.JoinIntent;
import me.mykindos.betterpvp.orchestration.model.PlayerRankSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueEntryView;
import me.mykindos.betterpvp.orchestration.model.QueueSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.Reservation;
import me.mykindos.betterpvp.orchestration.model.ReservationStatus;
import me.mykindos.betterpvp.orchestration.model.ServerCapacitySnapshot;
import me.mykindos.betterpvp.orchestration.policy.QueuePriorityCalculator;
import me.mykindos.betterpvp.orchestration.policy.QueuePriorityPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InMemoryOrchestrationGateway implements OrchestrationGateway {

    private static final Logger LOGGER = Logger.getLogger(InMemoryOrchestrationGateway.class.getName());
    private static final long RANK_SNAPSHOT_STALE_SECONDS = 15L;

    private final QueuePriorityPolicy priorityPolicy;
    private final long reservationTtlSeconds;
    private final long minimumRegularJoinIntervalMillis;
    private final PlayerQueuePolicyResolver playerQueuePolicyResolver;
    private final ManagedTargetResolver managedTargetResolver;
    private final ScheduledExecutorService scheduler;

    private final Map<QueueTarget, LinkedHashMap<UUID, ManagedQueueEntry>> queuesByTarget = new HashMap<>();
    private final Map<UUID, ManagedQueueEntry> queueEntriesByPlayer = new HashMap<>();
    private final Map<UUID, PlayerRankSnapshot> rankByPlayer = new HashMap<>();
    private final Map<UUID, QueueStatusUpdate> statusByPlayer = new HashMap<>();
    private final Map<String, ServerCapacitySnapshot> capacityByServer = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Reservation> reservationsById = new HashMap<>();
    private final Map<String, QueueState> queueStateOverrides = new HashMap<>();
    private final Map<String, Instant> nextRegularAdmissionAtByServer = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public InMemoryOrchestrationGateway(QueuePriorityPolicy priorityPolicy,
                                        long reservationTtlSeconds,
                                        long minimumRegularJoinIntervalMillis,
                                        PlayerQueuePolicyResolver playerQueuePolicyResolver,
                                        ManagedTargetResolver managedTargetResolver) {
        this.priorityPolicy = Objects.requireNonNull(priorityPolicy, "priorityPolicy");
        this.reservationTtlSeconds = reservationTtlSeconds;
        this.minimumRegularJoinIntervalMillis = Math.max(0L, minimumRegularJoinIntervalMillis);
        this.playerQueuePolicyResolver = Objects.requireNonNull(playerQueuePolicyResolver, "playerQueuePolicyResolver");
        this.managedTargetResolver = Objects.requireNonNull(managedTargetResolver, "managedTargetResolver");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, "orchestration-maintenance");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(this::runMaintenance, 1L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public synchronized CompletableFuture<AdmissionDecision> requestJoin(JoinIntent intent) {
        Objects.requireNonNull(intent, "intent");

        final Optional<QueueTarget> resolvedTarget = managedTargetResolver.resolve(intent.requestedServer());
        if (resolvedTarget.isEmpty()) {
            return CompletableFuture.completedFuture(AdmissionDecision.unmanaged("Target is not queue-managed"));
        }

        final QueueTarget target = resolvedTarget.get();
        final ManagedQueueEntry existingEntry = queueEntriesByPlayer.get(intent.playerUuid());
        if (existingEntry != null) {
            if (existingEntry.ticket().target().targetId().equalsIgnoreCase(target.targetId())) {
                final PlayerQueuePolicyResolver.ResolvedQueuePolicy existingPolicy = playerQueuePolicyResolver.resolve(intent.playerUuid(), rankByPlayer);
                if (existingPolicy.bypass()) {
                    removeExistingQueueEntry(intent.playerUuid());
                    statusByPlayer.remove(intent.playerUuid());
                    return CompletableFuture.completedFuture(AdmissionDecision.bypass(target, "Bypass access granted"));
                }
                return CompletableFuture.completedFuture(AdmissionDecision.queued(
                        target,
                        existingEntry.ticket().ticketId(),
                        "Already queued for admission"
                ));
            }

            removeExistingQueueEntry(intent.playerUuid());
            statusByPlayer.remove(intent.playerUuid());
        }

        final QueueStatusUpdate existingStatus = statusByPlayer.get(intent.playerUuid());
        if (existingStatus != null
                && existingStatus.readyToConnect()
                && existingStatus.queuedTarget().targetId().equalsIgnoreCase(target.targetId())) {
            return CompletableFuture.completedFuture(AdmissionDecision.granted(
                    target,
                    existingStatus.reservationId(),
                    "Already admitted"
            ));
        }

        final PlayerQueuePolicyResolver.ResolvedQueuePolicy resolvedPolicy = playerQueuePolicyResolver.resolve(intent.playerUuid(), rankByPlayer);

        if (resolvedPolicy.bypass()) {
            statusByPlayer.remove(intent.playerUuid());
            return CompletableFuture.completedFuture(AdmissionDecision.bypass(target, "Bypass access granted"));
        }

        final ServerCapacitySnapshot snapshot = capacityByServer.getOrDefault(
                target.serverName(),
                new ServerCapacitySnapshot(target.serverName(), QueueState.OPEN, 0, 0, 0, 0, Instant.now())
        );

        final QueueState targetState = queueState(target);
        if (targetState == QueueState.OFFLINE || targetState == QueueState.LOCKED) {
            return CompletableFuture.completedFuture(AdmissionDecision.denied(target, "Target server is not accepting players"));
        }

        final Instant now = Instant.now();
        if (targetState == QueueState.OPEN
                && snapshot.availableRegularSlots() > 0
                && canAdmitRegularPlayer(target.serverName(), now)) {
            final Reservation reservation = createReservation(intent.playerUuid(), target);
            updateReservedCapacity(target.serverName(), 1);
            recordRegularAdmission(target.serverName(), now);
            LOGGER.info(() -> "Granted immediate reservation " + reservation.reservationId()
                    + " for " + intent.playerUuid() + " to " + target.serverName());
            return CompletableFuture.completedFuture(AdmissionDecision.granted(target, reservation.reservationId(), "Reserved slot available"));
        }

        final ManagedQueueEntry entry = ManagedQueueEntry.from(new JoinIntent(
                intent.playerUuid(),
                intent.playerName(),
                intent.currentServer(),
                target.serverName(),
                intent.requestedAt()
        ), target, resolvedPolicy.basePriority());
        queuesByTarget.computeIfAbsent(target, ignored -> new LinkedHashMap<>())
                .put(intent.playerUuid(), entry);
        queueEntriesByPlayer.put(intent.playerUuid(), entry);

        refreshQueue(target, now);
        return CompletableFuture.completedFuture(AdmissionDecision.queued(target, entry.ticket().ticketId(), "Queued for admission"));
    }

    @Override
    public synchronized CompletableFuture<Void> leaveQueue(UUID playerUuid) {
        removeExistingQueueEntry(playerUuid);
        statusByPlayer.remove(playerUuid);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> updateCapacity(ServerCapacitySnapshot snapshot) {
        final ServerCapacitySnapshot existing = capacityByServer.get(snapshot.serverName());
        final int reservedRegular = existing == null ? snapshot.reservedRegular() : existing.reservedRegular();
        capacityByServer.put(snapshot.serverName(), recomputeOccupancy(new ServerCapacitySnapshot(
                snapshot.serverName(),
                snapshot.state(),
                snapshot.softCapacity(),
                snapshot.regularOnline(),
                snapshot.bypassOnline(),
                reservedRegular,
                snapshot.updatedAt()
        )));
        processAdmissionsForServer(snapshot.serverName(), Instant.now());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> confirmArrival(AdmissionConfirmation confirmation) {
        final PlayerRankSnapshot currentRankSnapshot = rankByPlayer.get(confirmation.playerUuid());
        if (currentRankSnapshot != null) {
            if (!currentRankSnapshot.currentServer().equalsIgnoreCase(confirmation.serverName())) {
                rankByPlayer.put(confirmation.playerUuid(), new PlayerRankSnapshot(
                        currentRankSnapshot.playerUuid(),
                        currentRankSnapshot.playerName(),
                        currentRankSnapshot.rank(),
                        confirmation.serverName(),
                        confirmation.confirmedAt()
                ));
                refreshCapacityForServer(currentRankSnapshot.currentServer(), confirmation.confirmedAt());
            }
        } else if (!confirmation.bypass()) {
            // Keep the arriving regular player visible to capacity checks before releasing their reservation.
            rankByPlayer.put(confirmation.playerUuid(), new PlayerRankSnapshot(
                    confirmation.playerUuid(),
                    confirmation.playerUuid().toString(),
                    "",
                    confirmation.serverName(),
                    confirmation.confirmedAt()
            ));
        }
        refreshCapacityForServer(confirmation.serverName(), confirmation.confirmedAt());

        if (!confirmation.bypass() && confirmation.reservationId() != null) {
            final Reservation reservation = reservationsById.get(confirmation.reservationId());
            if (reservation != null && reservation.status() == ReservationStatus.PENDING) {
                LOGGER.info(() -> "Consuming reservation " + confirmation.reservationId()
                        + " for " + confirmation.playerUuid() + " on " + confirmation.serverName());
                reservationsById.put(confirmation.reservationId(), new Reservation(
                        reservation.reservationId(),
                        reservation.playerUuid(),
                        reservation.target(),
                        reservation.expiresAt(),
                        ReservationStatus.CONSUMED
                ));
                updateReservedCapacity(confirmation.serverName(), -1);
            }
        }

        statusByPlayer.remove(confirmation.playerUuid());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> notifyDeparture(DepartureNotification notification) {
        LOGGER.info(() -> "Received departure for " + notification.playerUuid()
                + " from " + notification.serverName() + " bypass=" + notification.bypass());
        final PlayerRankSnapshot currentRankSnapshot = rankByPlayer.get(notification.playerUuid());
        if (currentRankSnapshot != null && currentRankSnapshot.currentServer().equalsIgnoreCase(notification.serverName())) {
            rankByPlayer.put(notification.playerUuid(), new PlayerRankSnapshot(
                    currentRankSnapshot.playerUuid(),
                    currentRankSnapshot.playerName(),
                    currentRankSnapshot.rank(),
                    "proxy",
                    notification.occurredAt()
            ));
        }

        refreshCapacityForServer(notification.serverName(), notification.occurredAt());
        processAdmissionsForServer(notification.serverName(), notification.occurredAt());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> upsertPlayerRank(PlayerRankSnapshot snapshot) {
        final PlayerRankSnapshot previous = rankByPlayer.put(snapshot.playerUuid(), snapshot);
        if (previous != null && !previous.currentServer().equalsIgnoreCase(snapshot.currentServer())) {
            refreshCapacityForServer(previous.currentServer(), Instant.now());
        }
        refreshCapacityForServer(snapshot.currentServer(), Instant.now());

        // If the player is now a bypass rank but still sitting in the queue, promote them immediately.
        final ManagedQueueEntry existingEntry = queueEntriesByPlayer.get(snapshot.playerUuid());
        if (existingEntry != null) {
            final PlayerQueuePolicyResolver.ResolvedQueuePolicy policy = playerQueuePolicyResolver.resolve(snapshot.playerUuid(), rankByPlayer);
            if (policy.bypass()) {
                removeExistingQueueEntry(snapshot.playerUuid());
                statusByPlayer.remove(snapshot.playerUuid());
                LOGGER.info(() -> "Upgraded queued player " + snapshot.playerUuid() + " to bypass after rank change to " + snapshot.rank());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> removePlayerRank(UUID playerUuid) {
        final PlayerRankSnapshot removed = rankByPlayer.remove(playerUuid);
        if (removed != null) {
            refreshCapacityForServer(removed.currentServer(), Instant.now());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Optional<PlayerRankSnapshot>> getPlayerRank(UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(rankByPlayer.get(playerUuid)));
    }

    @Override
    public synchronized CompletableFuture<Optional<QueueStatusUpdate>> getPlayerQueueStatus(UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(statusByPlayer.get(playerUuid)));
    }

    @Override
    public synchronized CompletableFuture<QueueSnapshot> getQueueSnapshot(QueueTarget target) {
        final Instant now = Instant.now();
        refreshQueue(target, now);

        final List<ManagedQueueEntry> orderedEntries = orderedEntries(target, now);
        final List<QueueEntryView> entries = new ArrayList<>(orderedEntries.size());
        int position = 1;
        for (ManagedQueueEntry entry : orderedEntries) {
            entries.add(new QueueEntryView(
                    entry.ticket().playerUuid(),
                    entry.playerName(),
                    position++,
                    entry.ticket().basePriority(),
                    effectivePriority(entry, now),
                    entry.ticket().enqueuedAt()
            ));
        }

        final ServerCapacitySnapshot snapshot = capacityByServer.getOrDefault(
                target.serverName(),
                new ServerCapacitySnapshot(target.serverName(), QueueState.OPEN, 0, 0, 0, 0, now)
        );
        LOGGER.info(() -> "Snapshot for " + target.serverName()
                + ": queued=" + entries.size()
                + ", regularOnline=" + snapshot.regularOnline()
                + ", bypassOnline=" + snapshot.bypassOnline()
                + ", reserved=" + snapshot.reservedRegular()
                + ", softCapacity=" + snapshot.softCapacity());

        return CompletableFuture.completedFuture(new QueueSnapshot(
                target,
                queueState(target),
                entries.size(),
                snapshot.softCapacity(),
                snapshot.regularOnline(),
                snapshot.bypassOnline(),
                snapshot.reservedRegular(),
                entries
        ));
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    @Override
    public synchronized CompletableFuture<Void> setQueueState(QueueTarget target, QueueState state) {
        queueStateOverrides.put(target.targetId(), state);
        processAdmissionsForServer(target.serverName(), Instant.now());
        refreshQueue(target, Instant.now());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Boolean> removeQueuedPlayer(UUID playerUuid) {
        final ManagedQueueEntry existing = queueEntriesByPlayer.get(playerUuid);
        if (existing == null) {
            statusByPlayer.remove(playerUuid);
            return CompletableFuture.completedFuture(false);
        }

        final QueueTarget target = existing.ticket().target();
        removeExistingQueueEntry(playerUuid);
        statusByPlayer.remove(playerUuid);
        refreshQueue(target, Instant.now());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public synchronized CompletableFuture<Boolean> admitQueuedPlayer(UUID playerUuid) {
        final ManagedQueueEntry existing = queueEntriesByPlayer.get(playerUuid);
        if (existing == null) {
            return CompletableFuture.completedFuture(false);
        }

        final QueueTarget target = existing.ticket().target();
        removeExistingQueueEntry(playerUuid);
        final Reservation reservation = createReservation(playerUuid, target);
        updateReservedCapacity(target.serverName(), 1);
        statusByPlayer.put(playerUuid, new QueueStatusUpdate(
                existing.ticket().playerUuid(),
                existing.ticket().currentServer(),
                target,
                true,
                true,
                1,
                1,
                effectivePriority(existing, Instant.now()),
                queueState(target),
                0L,
                existing.ticket().enqueuedAt(),
                existing.nextDisplayVersion(),
                reservation.reservationId()
        ));
        refreshQueue(target, Instant.now());
        return CompletableFuture.completedFuture(true);
    }

    private synchronized void runMaintenance() {
        final Instant now = Instant.now();
        expireReservations(now);
        refreshAllQueues(now);
        processAdmissions(now);
    }

    private void expireReservations(Instant now) {
        final List<String> expiredReservationIds = reservationsById.values().stream()
                .filter(reservation -> reservation.status() == ReservationStatus.PENDING && reservation.expiresAt().isBefore(now))
                .map(Reservation::reservationId)
                .toList();

        for (String reservationId : expiredReservationIds) {
            final Reservation reservation = reservationsById.get(reservationId);
            if (reservation == null) {
                continue;
            }

            reservationsById.put(reservationId, new Reservation(
                    reservation.reservationId(),
                    reservation.playerUuid(),
                    reservation.target(),
                    reservation.expiresAt(),
                    ReservationStatus.EXPIRED
            ));
            updateReservedCapacity(reservation.target().serverName(), -1);
            LOGGER.info(() -> "Expired reservation " + reservation.reservationId()
                    + " for " + reservation.playerUuid() + " to " + reservation.target().serverName());
        }
    }

    private void processAdmissions(Instant now) {
        final Collection<String> servers = capacityByServer.keySet();
        for (String serverName : servers) {
            processAdmissionsForServer(serverName, now);
        }
    }

    private void processAdmissionsForServer(String serverName, Instant now) {
        final ServerCapacitySnapshot snapshot = capacityByServer.get(serverName);
        if (snapshot == null || snapshot.state() != QueueState.OPEN || snapshot.availableRegularSlots() <= 0) {
            refreshQueuesForServer(serverName, now);
            return;
        }

        final List<QueueTarget> targets = queuesByTarget.keySet().stream()
                .filter(target -> target.serverName().equalsIgnoreCase(serverName))
                .sorted(Comparator.comparing(QueueTarget::targetId))
                .toList();

        int availableSlots = snapshot.availableRegularSlots();
        for (QueueTarget target : targets) {
            if (queueState(target) != QueueState.OPEN) {
                refreshQueue(target, now);
                continue;
            }

            while (availableSlots > 0 && canAdmitRegularPlayer(serverName, now)) {
                final List<ManagedQueueEntry> orderedEntries = orderedEntries(target, now);
                if (orderedEntries.isEmpty()) {
                    break;
                }

                final ManagedQueueEntry first = orderedEntries.getFirst();
                queuesByTarget.getOrDefault(target, new LinkedHashMap<>()).remove(first.ticket().playerUuid());
                queueEntriesByPlayer.remove(first.ticket().playerUuid());

                final Reservation reservation = createReservation(first.ticket().playerUuid(), target);
                updateReservedCapacity(serverName, 1);
                recordRegularAdmission(serverName, now);
                availableSlots--;
                LOGGER.info(() -> "Promoted queued player " + first.ticket().playerUuid()
                        + " with reservation " + reservation.reservationId()
                        + " to " + target.serverName());

                statusByPlayer.put(first.ticket().playerUuid(), new QueueStatusUpdate(
                        first.ticket().playerUuid(),
                        first.ticket().currentServer(),
                        target,
                        true,
                        true,
                        1,
                        1,
                        effectivePriority(first, now),
                        queueState(target),
                        0L,
                        first.ticket().enqueuedAt(),
                        first.nextDisplayVersion(),
                        reservation.reservationId()
                ));

                reservationsById.put(reservation.reservationId(), reservation);
            }

            refreshQueue(target, now);
        }
    }

    private void refreshAllQueues(Instant now) {
        for (QueueTarget target : queuesByTarget.keySet()) {
            refreshQueue(target, now);
        }
    }

    private void refreshQueuesForServer(String serverName, Instant now) {
        queuesByTarget.keySet().stream()
                .filter(target -> target.serverName().equalsIgnoreCase(serverName))
                .forEach(target -> refreshQueue(target, now));
    }

    private void refreshQueue(QueueTarget target, Instant now) {
        final List<ManagedQueueEntry> orderedEntries = orderedEntries(target, now);
        int position = 1;
        for (ManagedQueueEntry entry : orderedEntries) {
            statusByPlayer.put(entry.ticket().playerUuid(), new QueueStatusUpdate(
                    entry.ticket().playerUuid(),
                    entry.ticket().currentServer(),
                    target,
                    true,
                    false,
                    position++,
                    orderedEntries.size(),
                    effectivePriority(entry, now),
                    queueState(target),
                    null,
                    entry.ticket().enqueuedAt(),
                    entry.nextDisplayVersion(),
                    null
            ));
        }
    }

    private List<ManagedQueueEntry> orderedEntries(QueueTarget target, Instant now) {
        return queuesByTarget.getOrDefault(target, new LinkedHashMap<>())
                .values()
                .stream()
                .sorted(Comparator
                        .comparingInt((ManagedQueueEntry entry) -> effectivePriority(entry, now)).reversed()
                        .thenComparing(entry -> entry.ticket().enqueuedAt())
                        .thenComparing(entry -> entry.ticket().playerUuid()))
                .toList();
    }

    private int effectivePriority(ManagedQueueEntry entry, Instant now) {
        return QueuePriorityCalculator.effectivePriority(entry.ticket().basePriority(), entry.ticket().enqueuedAt(), now, priorityPolicy);
    }

    private QueueState capacityState(String serverName) {
        final ServerCapacitySnapshot snapshot = capacityByServer.get(serverName);
        return snapshot == null ? QueueState.OPEN : snapshot.state();
    }

    private QueueState queueState(QueueTarget target) {
        return queueStateOverrides.getOrDefault(target.targetId(), capacityState(target.serverName()));
    }

    private boolean canAdmitRegularPlayer(String serverName, Instant now) {
        final Instant nextAdmissionAt = nextRegularAdmissionAtByServer.get(serverName);
        return nextAdmissionAt == null || !now.isBefore(nextAdmissionAt);
    }

    private void recordRegularAdmission(String serverName, Instant now) {
        if (minimumRegularJoinIntervalMillis <= 0L) {
            nextRegularAdmissionAtByServer.remove(serverName);
            return;
        }

        nextRegularAdmissionAtByServer.put(serverName, now.plusMillis(minimumRegularJoinIntervalMillis));
    }

    private Reservation createReservation(UUID playerUuid, QueueTarget target) {
        final String reservationId = UUID.randomUUID().toString();
        final Reservation reservation = new Reservation(
                reservationId,
                playerUuid,
                target,
                Instant.now().plusSeconds(reservationTtlSeconds),
                ReservationStatus.PENDING
        );
        reservationsById.put(reservationId, reservation);
        return reservation;
    }

    private void updateReservedCapacity(String serverName, int delta) {
        final ServerCapacitySnapshot current = capacityByServer.get(serverName);
        if (current == null) {
            return;
        }

        final int oldReserved = current.reservedRegular();
        final ServerCapacitySnapshot updated = recomputeOccupancy(new ServerCapacitySnapshot(
                current.serverName(),
                current.state(),
                current.softCapacity(),
                current.regularOnline(),
                current.bypassOnline(),
                Math.max(0, current.reservedRegular() + delta),
                Instant.now()
        ));
        capacityByServer.put(serverName, updated);
        LOGGER.info(() -> "Reserved slots for " + serverName
                + " changed from " + oldReserved
                + " by " + delta
                + " to " + updated.reservedRegular());
    }

    private void refreshCapacityForServer(String serverName, Instant now) {
        final ServerCapacitySnapshot current = capacityByServer.get(serverName);
        if (current == null) {
            return;
        }

        capacityByServer.put(serverName, recomputeOccupancy(new ServerCapacitySnapshot(
                current.serverName(),
                current.state(),
                current.softCapacity(),
                current.regularOnline(),
                current.bypassOnline(),
                current.reservedRegular(),
                now
        )));
    }

    private ServerCapacitySnapshot recomputeOccupancy(ServerCapacitySnapshot snapshot) {
        final Instant occupancyUpdatedAt = snapshot.updatedAt();
        evictStaleRankSnapshots(occupancyUpdatedAt);

        int regularOnline = 0;
        int bypassOnline = 0;

        for (PlayerRankSnapshot playerRankSnapshot : rankByPlayer.values()) {
            if (playerRankSnapshot.updatedAt().plusSeconds(RANK_SNAPSHOT_STALE_SECONDS).isBefore(occupancyUpdatedAt)) {
                continue;
            }

            if (!playerRankSnapshot.currentServer().equalsIgnoreCase(snapshot.serverName())) {
                continue;
            }

            final PlayerQueuePolicyResolver.ResolvedQueuePolicy policy =
                    playerQueuePolicyResolver.resolve(playerRankSnapshot.playerUuid(), rankByPlayer);
            if (policy.bypass()) {
                bypassOnline++;
            } else {
                regularOnline++;
            }
        }

        return new ServerCapacitySnapshot(
                snapshot.serverName(),
                snapshot.state(),
                snapshot.softCapacity(),
                regularOnline,
                bypassOnline,
                snapshot.reservedRegular(),
                snapshot.updatedAt()
        );
    }

    private void evictStaleRankSnapshots(Instant now) {
        rankByPlayer.entrySet().removeIf(entry ->
                entry.getValue().updatedAt().plusSeconds(RANK_SNAPSHOT_STALE_SECONDS).isBefore(now));
    }

    private void removeExistingQueueEntry(UUID playerUuid) {
        final ManagedQueueEntry existing = queueEntriesByPlayer.remove(playerUuid);
        if (existing == null) {
            return;
        }

        final LinkedHashMap<UUID, ManagedQueueEntry> queue = queuesByTarget.get(existing.ticket().target());
        if (queue != null) {
            queue.remove(playerUuid);
            if (queue.isEmpty()) {
                queuesByTarget.remove(existing.ticket().target());
            }
        }
    }
}
