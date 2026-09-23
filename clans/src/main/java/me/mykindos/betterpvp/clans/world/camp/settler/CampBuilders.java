package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a camp's Builders bring to a job: their rarity's numbers, what their trade adds on the jobs it suits, and
 * their traits. The good part of a trait grows with its settler's trait strength, a trade-off's cost never does.
 */
@Singleton
public class CampBuilders {

    private static final long HOUR_MILLIS = 3_600_000L;

    private final SettlerConfig config;

    @Inject
    public CampBuilders(@NotNull SettlerConfig config) {
        this.config = config;
    }

    public @NotNull BuilderStats stats(@NotNull Settler settler, @NotNull Job job, @NotNull List<Settler> crew) {
        final SettlerConfig.BuilderNumbers base = config.builder(settler.getRarity());
        final double strength = strength(settler);
        final Optional<SettlerConfig.Trade> trade = config.trade(settler.getSpecialty());

        double workforce = base.getWorkforce();
        double bonus = 0;
        double stats = 1;
        if (trade.isPresent()) {
            workforce += trade.get().getWorkforce();
            if (trade.get().getResource() != null && mostly(job, trade.get().getResource())) {
                bonus += trade.get().getSpecialty();
            }
        }

        if (settler.hasTrait(CampTraits.STEADY_HANDS)) {
            workforce += Math.round(number(CampTraits.STEADY_HANDS, "workforce", 1) * strength);
        }
        if (settler.hasTrait(CampTraits.BRAWNY)) {
            workforce += Math.round(number(CampTraits.BRAWNY, "workforce", 2) * strength);
            bonus += number(CampTraits.BRAWNY, "speed", -0.10);
        }
        if (settler.hasTrait(CampTraits.TIRELESS)
                && job.getDurationMillis() > number(CampTraits.TIRELESS, "min-hours", 2) * HOUR_MILLIS) {
            bonus += number(CampTraits.TIRELESS, "speed", 0.15) * strength;
        }
        if (settler.hasTrait(CampTraits.QUICK_STUDY)) {
            final double perFive = number(CampTraits.QUICK_STUDY, "speed-per-five-jobs", 0.05) * strength;
            bonus += Math.min(number(CampTraits.QUICK_STUDY, "max-speed", 0.25) * strength,
                    (double) (settler.getJobsFinished() / 5) * perFive);
        }
        if (settler.hasTrait(CampTraits.PATCHER) && job.getKind() == JobKind.REPAIR) {
            bonus += number(CampTraits.PATCHER, "speed", 0.20) * strength;
        }
        if (settler.hasTrait(CampTraits.ARCHITECTS_EYE) && job.getKind() == JobKind.ADVANCE) {
            bonus += number(CampTraits.ARCHITECTS_EYE, "speed", 0.15) * strength;
        }
        if (settler.hasTrait(CampTraits.LONER)) {
            bonus += crew.size() <= 1
                    ? number(CampTraits.LONER, "alone", 0.40) * strength
                    : number(CampTraits.LONER, "in-crew", -0.20);
        }
        if (settler.hasTrait(CampTraits.GREEDY)) {
            bonus += number(CampTraits.GREEDY, "speed", 0.15) * strength;
        }
        bonus += foreman(settler, crew);
        if (settler.hasTrait(CampTraits.PRODIGY)) {
            stats *= 1 + number(CampTraits.PRODIGY, "stats", 0.25) * strength;
        }
        if (settler.hasTrait(CampTraits.HOMESICK)) {
            stats *= 1 + number(CampTraits.HOMESICK, "stats", 0.10) * strength;
        }

        return new BuilderStats((int) Math.max(0, Math.round(workforce * stats)),
                Math.max(0, base.getSpeed() * (1 + bonus) * stats), base.getEfficiency(),
                settler.getSpecialty(), trade.map(SettlerConfig.Trade::getCompatible).orElse(Set.of()));
    }

    /** The share of a job's cost a finished crew hands back: the best Frugal on it, and the best Patcher on a repair. */
    public double refund(@NotNull Job job, @NotNull List<Settler> crew) {
        double frugal = 0;
        double patcher = 0;
        for (Settler member : crew) {
            if (member.hasTrait(CampTraits.FRUGAL)) {
                frugal = Math.max(frugal, number(CampTraits.FRUGAL, "refund", 0.05) * strength(member));
            }
            if (member.hasTrait(CampTraits.PATCHER) && job.getKind() == JobKind.REPAIR) {
                patcher = Math.max(patcher, number(CampTraits.PATCHER, "refund", 0.20) * strength(member));
            }
        }
        return Math.min(1, frugal + patcher);
    }

    /** The speed the strongest Foreman on the crew gives everyone else. Only one Foreman counts. */
    private double foreman(@NotNull Settler settler, @NotNull List<Settler> crew) {
        return crew.stream()
                .filter(member -> !member.getId().equals(settler.getId()))
                .filter(member -> member.hasTrait(CampTraits.FOREMAN))
                .mapToDouble(member -> number(CampTraits.FOREMAN, "crew-speed", 0.10) * strength(member))
                .max()
                .orElse(0);
    }

    /** Whether most of what {@code job} cost was {@code resource}. A tie is no one resource. */
    static boolean mostly(@NotNull Job job, @NotNull String resource) {
        final Map<String, Integer> amounts = job.getSpent().getAmounts();
        final int wanted = amounts.getOrDefault(resource, 0);
        return wanted > 0 && amounts.entrySet().stream()
                .noneMatch(entry -> !entry.getKey().equals(resource) && entry.getValue() >= wanted);
    }

    private double strength(@NotNull Settler settler) {
        return config.getTable().rarity(settler.getRarity()).getTraitStrength();
    }

    private double number(@NotNull String trait, @NotNull String number, double fallback) {
        return config.trait(trait, number, fallback);
    }
}
