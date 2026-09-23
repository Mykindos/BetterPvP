package me.mykindos.betterpvp.core.world.settler.crew;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * How fast a crew works together. The fastest Builder counts fully and every other one at its efficiency, so adding
 * Builders helps less each time. A Builder whose compatible trade is already on the crew counts fully however it
 * ranks, and gets a bonus on top. The total is capped.
 */
public final class CrewSpeed {

    private CrewSpeed() {
    }

    /** How many times faster than its listed time the crew runs a job, or 0 for no crew. */
    public static double of(@NotNull List<BuilderStats> crew, @NotNull CrewLimits limits) {
        final List<BuilderStats> ranked = crew.stream()
                .sorted(Comparator.comparingDouble(BuilderStats::getSpeed).reversed())
                .toList();
        double total = 0;
        for (int i = 0; i < ranked.size(); i++) {
            final BuilderStats builder = ranked.get(i);
            if (compatible(builder, ranked)) {
                total += builder.getSpeed() * (1 + limits.getCompatibleBonus());
            } else {
                total += i == 0 ? builder.getSpeed() : builder.getSpeed() * builder.getEfficiency();
            }
        }
        return Math.min(total, limits.getMaxSpeed());
    }

    public static int workforce(@NotNull List<BuilderStats> crew) {
        return crew.stream().mapToInt(BuilderStats::getWorkforce).sum();
    }

    private static boolean compatible(@NotNull BuilderStats builder, @NotNull List<BuilderStats> crew) {
        return crew.stream()
                .filter(other -> other != builder)
                .map(BuilderStats::getTrade)
                .filter(Objects::nonNull)
                .anyMatch(builder.getCompatible()::contains);
    }
}
