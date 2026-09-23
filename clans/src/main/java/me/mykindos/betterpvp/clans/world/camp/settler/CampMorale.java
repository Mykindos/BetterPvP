package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerDeparture;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.morale.FoodSource;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleModel;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Set;

/**
 * How a camp's settlers feel. Food and a Bard raise everyone. Wages owed to strikers, a profession with nowhere to
 * work and recent dismissals bring them down. Beloved caps what idleness and dismissals can do, Content keeps a
 * settler from falling too far, Moody doubles every swing, Homesick costs a little, and Loyal settlers never leave.
 */
@Singleton
public class CampMorale implements MoraleModel {

    private static final double HOUR_MILLIS = 3_600_000.0;

    private final SettlerConfig config;
    private final CampWideTraits campWide;
    private final Set<FoodSource> food;

    @Inject
    public CampMorale(@NotNull SettlerConfig config, @NotNull CampWideTraits campWide, @NotNull Set<FoodSource> food) {
        this.config = config;
        this.campWide = campWide;
        this.food = food;
    }

    @Override
    public int morale(@NotNull SiteKey site, @NotNull Settler settler, @NotNull Roster roster, long now) {
        double morale = food(site, roster) + campWide.best(roster, CampTraits.BARD, "morale", 5);
        if (!roster.inState(SettlerState.STRIKING).isEmpty()) {
            morale += number("unpaid", -25);
        }

        double troubles = idle(settler, now) + dismissals(roster, now);
        if (campWide.any(roster, CampTraits.BELOVED)) {
            troubles = Math.max(config.trait(CampTraits.BELOVED, "floor", -10), troubles);
        }
        morale += troubles;
        if (settler.hasTrait(CampTraits.HOMESICK)) {
            morale += config.trait(CampTraits.HOMESICK, "morale", -10);
        }

        if (settler.hasTrait(CampTraits.MOODY)) {
            morale *= config.trait(CampTraits.MOODY, "swing", 2);
        }
        if (settler.hasTrait(CampTraits.CONTENT)) {
            morale = Math.max(config.trait(CampTraits.CONTENT, "floor", -20), morale);
        }
        return (int) Math.round(morale);
    }

    @Override
    public int leaveBelow() {
        return (int) number("leave-below", -40);
    }

    @Override
    public @NotNull Duration leaveAfter() {
        return Duration.ofMinutes((long) (number("leave-after-hours", 48) * 60));
    }

    @Override
    public boolean mayLeave(@NotNull Settler settler) {
        return !settler.hasTrait(CampTraits.LOYAL);
    }

    /** The best food anything gives, made stronger by the best Cook, up to the most food can add. */
    private double food(@NotNull SiteKey site, @NotNull Roster roster) {
        final int best = food.stream().mapToInt(source -> source.morale(site)).max().orElse(0);
        if (best <= 0) {
            return 0;
        }
        final double cook = campWide.best(roster, CampTraits.COOK, "food", 0.25);
        return Math.min(number("food-max", 30), best * (1 + cook));
    }

    /** A settler with a profession and nowhere to work grows restless once it has waited long enough. */
    double idle(@NotNull Settler settler, long now) {
        if (settler.getProfession() == null || settler.getState() != SettlerState.IDLE) {
            return 0;
        }
        final long since = Math.max(settler.getStateSince(), settler.getJoinedAt());
        final double hours = (now - since) / HOUR_MILLIS - number("idle-after-hours", 24);
        if (hours <= 0) {
            return 0;
        }
        return Math.max(number("idle-floor", -30), Math.floor(hours) * number("idle-per-hour", -1));
    }

    /** Every recent dismissal weighs on everyone, less as it fades. */
    double dismissals(@NotNull Roster roster, long now) {
        final double window = number("dismissal-hours", 48) * HOUR_MILLIS;
        double total = 0;
        for (SettlerDeparture departure : roster.departedSince((long) (now - window), SettlerLeaveReason.DISMISSED)) {
            total += number("dismissal", -10) * Math.max(0, 1 - (now - departure.getAt()) / window);
        }
        return Math.max(number("dismissal-floor", -30), total);
    }

    private double number(@NotNull String number, double fallback) {
        return config.morale(number, fallback);
    }
}
