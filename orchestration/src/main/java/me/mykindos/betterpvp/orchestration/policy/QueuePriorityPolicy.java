package me.mykindos.betterpvp.orchestration.policy;

public record QueuePriorityPolicy(
        long boostIntervalSeconds,
        int boostAmount
) {

    public QueuePriorityPolicy {
        if (boostIntervalSeconds <= 0) {
            throw new IllegalArgumentException("boostIntervalSeconds must be > 0");
        }
        if (boostAmount < 0) {
            throw new IllegalArgumentException("boostAmount must be >= 0");
        }
    }

    public int waitingBoost(long waitedSeconds) {
        if (waitedSeconds <= 0 || boostAmount == 0) {
            return 0;
        }

        return (int) (waitedSeconds / boostIntervalSeconds) * boostAmount;
    }
}
