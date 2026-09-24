package me.mykindos.betterpvp.core.world.settler.crew;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

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
        double total = 0;
        for (double contribution : contributions(crew, limits)) {
            total += contribution;
        }
        return Math.min(total, limits.getMaxSpeed());
    }

    /**
     * What each Builder adds to the crew's speed, in the order given. When the crew is over the cap, every share is
     * scaled down alike so they add up to the cap.
     */
    public static double @NotNull [] contributions(@NotNull List<BuilderStats> crew, @NotNull CrewLimits limits) {
        final List<Integer> ranked = IntStream.range(0, crew.size()).boxed()
                .sorted(Comparator.comparingDouble((Integer index) -> crew.get(index).getSpeed()).reversed())
                .toList();
        final double[] contributions = new double[crew.size()];
        double total = 0;
        for (int rank = 0; rank < ranked.size(); rank++) {
            final int index = ranked.get(rank);
            final BuilderStats builder = crew.get(index);
            if (compatible(builder, crew)) {
                contributions[index] = builder.getSpeed() * (1 + limits.getCompatibleBonus());
            } else {
                contributions[index] = rank == 0 ? builder.getSpeed() : builder.getSpeed() * builder.getEfficiency();
            }
            total += contributions[index];
        }
        if (total > limits.getMaxSpeed()) {
            final double scale = limits.getMaxSpeed() / total;
            for (int i = 0; i < contributions.length; i++) {
                contributions[i] *= scale;
            }
        }
        return contributions;
    }

    /**
     * The part of each Builder's own Speed the job does not get, in the order given: what it loses by not being the
     * fastest, and its share of whatever the crew brings over the cap. Never below zero.
     */
    public static double @NotNull [] wasted(@NotNull List<BuilderStats> crew, @NotNull CrewLimits limits) {
        final double[] contributions = contributions(crew, limits);
        final double[] wasted = new double[crew.size()];
        for (int i = 0; i < wasted.length; i++) {
            wasted[i] = Math.max(0, crew.get(i).getSpeed() - contributions[i]);
        }
        return wasted;
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
