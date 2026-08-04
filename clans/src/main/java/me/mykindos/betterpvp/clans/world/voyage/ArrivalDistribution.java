package me.mykindos.betterpvp.clans.world.voyage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Which of a destination's docks a crew lands at.
 * <p>
 * Chosen once per voyage and applied to the whole crew — that is what parties are for. Left to chance per player, a
 * place like Aldenmark with several docks would scatter a group across all of them, and travelling together would
 * achieve nothing.
 * <p>
 * The choice belongs to the destination rather than the traveller: somewhere with a formal harbour can land everyone at
 * the same quay, somewhere sprawling can spread arrivals around.
 */
@FunctionalInterface
public interface ArrivalDistribution {

    /**
     * Picks a dock.
     *
     * @param candidateNames the destination's arrival points, in map order
     * @return an index into {@code candidateNames}, always within range for a non-empty list
     */
    int select(@NotNull List<String> candidateNames);

    /** Scatters arrivals. The default: a coastline with several landings should use all of them. */
    static @NotNull ArrivalDistribution random() {
        return candidates -> candidates.isEmpty() ? 0 : ThreadLocalRandom.current().nextInt(candidates.size());
    }

    /** Spreads arrivals evenly in turn, so no dock stays quiet while another is crowded. */
    static @NotNull ArrivalDistribution roundRobin() {
        final AtomicInteger next = new AtomicInteger();
        return candidates -> candidates.isEmpty() ? 0 : Math.floorMod(next.getAndIncrement(), candidates.size());
    }

    /**
     * Always the same dock — for a place with a front door, like spawn.
     * <p>
     * Falls back to the first candidate if that dock is missing, rather than refusing to land anyone: a renamed marker
     * should mean people arrive somewhere slightly wrong, not that the destination becomes unreachable.
     */
    static @NotNull ArrivalDistribution fixed(@NotNull String dockName) {
        final String wanted = dockName.toLowerCase(Locale.ROOT);
        return candidates -> {
            for (int index = 0; index < candidates.size(); index++) {
                if (candidates.get(index).toLowerCase(Locale.ROOT).equals(wanted)) {
                    return index;
                }
            }
            return 0;
        };
    }

    /** Reads a distribution by name, as authored in config. Anything unrecognised falls back to {@link #random()}. */
    static @NotNull ArrivalDistribution byName(@NotNull String name) {
        return switch (name.toLowerCase(Locale.ROOT).trim()) {
            case "round-robin", "roundrobin" -> roundRobin();
            case "first", "fixed" -> candidates -> 0;
            default -> random();
        };
    }
}
