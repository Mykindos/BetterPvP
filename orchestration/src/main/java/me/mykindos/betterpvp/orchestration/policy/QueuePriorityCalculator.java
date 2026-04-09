package me.mykindos.betterpvp.orchestration.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class QueuePriorityCalculator {

    private QueuePriorityCalculator() {
    }

    public static int effectivePriority(int basePriority, Instant enqueuedAt, Instant now, QueuePriorityPolicy policy) {
        Objects.requireNonNull(enqueuedAt, "enqueuedAt");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(policy, "policy");

        if (!now.isAfter(enqueuedAt)) {
            return basePriority;
        }

        final long waitedSeconds = Duration.between(enqueuedAt, now).getSeconds();
        return basePriority + policy.waitingBoost(waitedSeconds);
    }
}
