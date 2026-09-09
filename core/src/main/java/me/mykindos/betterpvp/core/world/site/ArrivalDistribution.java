package me.mykindos.betterpvp.core.world.site;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Which of a world's arrival points a party is put down at.
 * <p>
 * Chosen once for the whole party rather than once per player, which is what parties are for. Choosing per player
 * would scatter a group across every marker in a world that has several, and arriving together would achieve nothing.
 * <p>
 * The choice belongs to the site rather than the player, so a site with one entrance can put everybody in the same
 * spot and a large one can spread them out.
 */
@FunctionalInterface
public interface ArrivalDistribution {

    /**
     * Picks one of the points.
     *
     * @param candidateNames the site's arrival points, in map order
     * @return an index into {@code candidateNames}, always within range for a non-empty list
     */
    int select(@NotNull List<String> candidateNames);

    /** Picks at random, which is the default, so a world with several markers uses all of them. */
    static @NotNull ArrivalDistribution random() {
        return candidates -> candidates.isEmpty() ? 0 : ThreadLocalRandom.current().nextInt(candidates.size());
    }

    /** Takes each in turn, so no point stays unused while another is crowded. */
    static @NotNull ArrivalDistribution roundRobin() {
        final AtomicInteger next = new AtomicInteger();
        return candidates -> candidates.isEmpty() ? 0 : Math.floorMod(next.getAndIncrement(), candidates.size());
    }

    /**
     * Always the same point, for a site with one way in.
     * <p>
     * Falls back to the first candidate when that point is missing, rather than refusing to place anybody. A renamed
     * marker should mean people arrive somewhere slightly wrong, not that the site becomes unreachable.
     */
    static @NotNull ArrivalDistribution fixed(@NotNull String pointName) {
        final String wanted = pointName.toLowerCase(Locale.ROOT);
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
