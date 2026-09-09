package me.mykindos.betterpvp.core.world.site;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Picks which instance a party joins when several would admit them. Returns an index into the candidate list.
 */
@FunctionalInterface
public interface Selection {

    /**
     * @param occupantCounts how full each candidate is, in a stable order
     * @return an index into {@code occupantCounts}, always in range for a non-empty list
     */
    int select(@NotNull List<Integer> occupantCounts);

    /**
     * Sends a party to the busiest instance that will still take them, so a quiet site collapses onto one world.
     */
    static @NotNull Selection fillFirst() {
        return counts -> {
            int best = 0;
            for (int index = 1; index < counts.size(); index++) {
                if (counts.get(index) > counts.get(best)) {
                    best = index;
                }
            }
            return best;
        };
    }

    /**
     * Sends a party to the emptiest instance, keeping load even across all of them.
     */
    static @NotNull Selection spread() {
        return counts -> {
            int best = 0;
            for (int index = 1; index < counts.size(); index++) {
                if (counts.get(index) < counts.get(best)) {
                    best = index;
                }
            }
            return best;
        };
    }

    /**
     * @return a rule of the named kind, or {@code null} if there is no such rule
     */
    static @Nullable Selection byName(@NotNull String name) {
        return Registry.RULES.get(name.toLowerCase().replace('_', '-'));
    }

    final class Registry {

        private static final Map<String, Selection> RULES = new ConcurrentHashMap<>(Map.of(
                "fill-first", fillFirst(),
                "spread", spread()));

        private Registry() {
        }
    }
}
