package me.mykindos.betterpvp.core.item.runeslot;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

/**
 * Represents a purity-based distribution for rolling rune slots.
 * Contains two separate weight maps:
 * <ul>
 *     <li>socketWeights: weights for rolling sockets (0-4)</li>
 *     <li>maxSocketWeights: weights for rolling maxSockets (0-4)</li>
 * </ul>
 * <p>
 * The rolling process ensures that maxSockets &gt;= sockets.
 */
@Getter
public class RuneSlotDistribution {

    private final ItemPurity purity;
    private final Map<Integer, Integer> socketWeights;
    private final Map<Integer, Integer> maxSocketWeights;
    private final Random random = new Random();

    /**
     * Creates a new rune slot distribution for a specific purity.
     *
     * @param purity The purity level this distribution applies to
     * @param socketWeights Weight map for rolling sockets (keys 0-4)
     * @param maxSocketWeights Weight map for rolling maxSockets (keys 0-4)
     * @throws IllegalArgumentException if weights are invalid
     */
    public RuneSlotDistribution(@NotNull ItemPurity purity,
                                @NotNull Map<Integer, Integer> socketWeights,
                                @NotNull Map<Integer, Integer> maxSocketWeights) {
        this.purity = Objects.requireNonNull(purity, "Purity cannot be null");
        this.socketWeights = validateWeights(socketWeights, "socket");
        this.maxSocketWeights = validateWeights(maxSocketWeights, "maxSocket");
    }

    /**
     * Validates that a weight map is properly formatted.
     *
     * @param weights The weight map to validate
     * @param name Name for error messages
     * @return The validated weight map
     * @throws IllegalArgumentException if weights are invalid
     */
    private Map<Integer, Integer> validateWeights(Map<Integer, Integer> weights, String name) {
        Objects.requireNonNull(weights, name + " weights cannot be null");

        if (weights.isEmpty()) {
            throw new IllegalArgumentException(name + " weights cannot be empty");
        }

        // Check that all keys are 0-4
        for (Integer key : weights.keySet()) {
            if (key < 0 || key > 4) {
                throw new IllegalArgumentException(
                    name + " weight key must be 0-4, got: " + key
                );
            }
        }

        // Check that all values are non-negative
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException(
                    name + " weight for " + entry.getKey() + " cannot be negative: " + entry.getValue()
                );
            }
        }

        return new TreeMap<>(weights);
    }

    /**
     * Rolls both sockets and maxSockets for this distribution.
     * Ensures maxSockets &gt;= sockets.
     *
     * @return RuneSlotRoll containing both values
     */
    @NotNull
    public RuneSlotRoll roll() {
        int sockets = rollFromWeights(socketWeights);
        int maxSockets = rollMaxSocketsGivenSockets(sockets);
        return new RuneSlotRoll(sockets, maxSockets);
    }

    /**
     * Rolls maxSockets given that sockets has already been determined.
     * Ensures maxSockets &gt;= sockets by filtering the weight map.
     *
     * @param sockets The already-rolled sockets value
     * @return The rolled maxSockets value (guaranteed &gt;= sockets)
     */
    private int rollMaxSocketsGivenSockets(int sockets) {
        // Filter maxSocketWeights to only include values >= sockets
        Map<Integer, Integer> filteredWeights = new TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : maxSocketWeights.entrySet()) {
            if (entry.getKey() >= sockets) {
                filteredWeights.put(entry.getKey(), entry.getValue());
            }
        }

        // If no valid weights (shouldn't happen with proper data), default to sockets
        if (filteredWeights.isEmpty()) {
            return sockets;
        }

        return rollFromWeights(filteredWeights);
    }

    /**
     * Rolls a value from a weight map using weighted random selection.
     *
     * @param weights The weight map
     * @return The rolled value
     */
    private int rollFromWeights(Map<Integer, Integer> weights) {
        // Calculate total weight
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();

        // If total weight is 0, return first key (shouldn't happen with valid data)
        if (totalWeight <= 0) {
            return weights.keySet().iterator().next();
        }

        // Roll a random number
        int roll = random.nextInt(totalWeight);

        // Find which entry this roll corresponds to
        int cumulative = 0;
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }

        // Fallback (shouldn't reach here)
        return weights.keySet().stream().max(Integer::compareTo).orElse(4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuneSlotDistribution that = (RuneSlotDistribution) o;
        return purity == that.purity
            && Objects.equals(socketWeights, that.socketWeights)
            && Objects.equals(maxSocketWeights, that.maxSocketWeights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(purity, socketWeights, maxSocketWeights);
    }

    @Override
    public String toString() {
        return "RuneSlotDistribution{" +
                "purity=" + purity +
                ", socketWeights=" + socketWeights +
                ", maxSocketWeights=" + maxSocketWeights +
                '}';
    }

    /**
     * Result of rolling rune slots.
     */
    @Getter
    public static class RuneSlotRoll {
        private final int sockets;
        private final int maxSockets;

        public RuneSlotRoll(int sockets, int maxSockets) {
            if (maxSockets < sockets) {
                throw new IllegalArgumentException(
                    "maxSockets (" + maxSockets + ") cannot be less than sockets (" + sockets + ")"
                );
            }
            this.sockets = sockets;
            this.maxSockets = maxSockets;
        }

        @Override
        public String toString() {
            return sockets + "/" + maxSockets;
        }
    }
}
