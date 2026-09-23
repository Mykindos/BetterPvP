package me.mykindos.betterpvp.core.world.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Every change to a roster, checked against its site's caps: settlers joining, leaving, and being given a place to
 * work or taken off it. Each change asks the site's {@link SettlerSite} for its roster and fires an event for whatever
 * shows it in the world.
 */
@Singleton
public class SettlerService {

    /** How long a departure is remembered, for anything that looks back at who left. */
    private static final long DEPARTURES_KEPT_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private final ProfessionRegistry professions;
    private final LongSupplier clock;
    private final Map<String, SettlerSite> sites = new HashMap<>();

    @Inject
    public SettlerService(@NotNull ProfessionRegistry professions) {
        this(professions, System::currentTimeMillis);
    }

    SettlerService(@NotNull ProfessionRegistry professions, @NotNull LongSupplier clock) {
        this.professions = professions;
        this.clock = clock;
    }

    /** Lets every instance of the site {@code siteId} have settlers. */
    public void register(@NotNull String siteId, @NotNull SettlerSite site) {
        sites.put(siteId, site);
    }

    /** What the owner of {@code key}'s site supplies, if settlers can live there. */
    public @NotNull Optional<SettlerSite> site(@NotNull SiteKey key) {
        return Optional.ofNullable(sites.get(key.getSiteId()));
    }

    public @NotNull Optional<Roster> roster(@NotNull SiteKey key) {
        return Optional.ofNullable(sites.get(key.getSiteId())).flatMap(site -> site.roster(key));
    }

    /** How many settlers {@code key} can have in all, or 0 where settlers cannot live. */
    public int populationCap(@NotNull SiteKey key) {
        final SettlerSite site = sites.get(key.getSiteId());
        return site == null ? 0 : site.populationCap(key);
    }

    public @NotNull OptionalInt workingCap(@NotNull SiteKey key, @NotNull String profession) {
        final SettlerSite site = sites.get(key.getSiteId());
        return site == null ? OptionalInt.empty() : site.workingCap(key, profession);
    }

    /** Adds {@code settler} to the roster of {@code key} if there is room for one more. */
    public @NotNull SettlerResult grant(@NotNull SiteKey key, @NotNull Settler settler) {
        final SettlerSite site = sites.get(key.getSiteId());
        final Roster roster = site == null ? null : site.roster(key).orElse(null);
        if (roster == null) {
            return SettlerResult.refused("core.settler.not_loaded");
        }
        final int cap = site.populationCap(key);
        if (roster.size() >= cap) {
            return SettlerResult.refused("core.settler.population_full", Component.text(cap));
        }

        final long now = clock.getAsLong();
        settler.setAssignment(null);
        settler.setState(SettlerState.IDLE);
        settler.setJoinedAt(now);
        settler.setStateSince(now);
        roster.getSettlers().add(settler);
        site.changed(key);
        UtilServer.callEvent(new SettlerJoinedEvent(key, settler));
        return SettlerResult.done(settler);
    }

    /** Sends a settler away for good. */
    public @NotNull SettlerResult dismiss(@NotNull SiteKey key, @NotNull UUID settlerId) {
        return remove(key, settlerId, SettlerLeaveReason.DISMISSED);
    }

    /** Takes a settler off the roster for good, for {@code reason}. */
    public @NotNull SettlerResult remove(@NotNull SiteKey key, @NotNull UUID settlerId,
                                         @NotNull SettlerLeaveReason reason) {
        final SettlerSite site = sites.get(key.getSiteId());
        final Roster roster = site == null ? null : site.roster(key).orElse(null);
        final Settler settler = roster == null ? null : roster.find(settlerId).orElse(null);
        if (settler == null) {
            return SettlerResult.refused("core.settler.not_found");
        }

        final long now = clock.getAsLong();
        roster.getSettlers().remove(settler);
        roster.getDepartures().removeIf(departure -> now - departure.getAt() > DEPARTURES_KEPT_MILLIS);
        roster.getDepartures().add(new SettlerDeparture(settler.getName(), settler.getRarity(), reason, now));
        settler.changeState(SettlerState.LEAVING, now);
        site.changed(key);
        UtilServer.callEvent(new SettlerLeftEvent(key, settler, reason));
        return SettlerResult.done(settler);
    }

    /**
     * Puts a settler to work at {@code workplace}: a construction job for a profession that builds, or the one
     * workplace its profession works at. Moving between workplaces does not count against the working cap twice.
     */
    public @NotNull SettlerResult assign(@NotNull SiteKey key, @NotNull UUID settlerId, @NotNull String workplace) {
        final SettlerSite site = sites.get(key.getSiteId());
        final Roster roster = site == null ? null : site.roster(key).orElse(null);
        final Settler settler = roster == null ? null : roster.find(settlerId).orElse(null);
        if (settler == null) {
            return SettlerResult.refused("core.settler.not_found");
        }
        final Profession profession = settler.getProfession() == null ? null
                : professions.find(settler.getProfession()).orElse(null);
        if (profession == null) {
            return SettlerResult.refused("core.settler.no_profession");
        }
        if (profession.getWorkplaceKind() == WorkplaceKind.WORKPLACE && !workplace.equals(profession.getWorkplace())) {
            return SettlerResult.refused("core.settler.wrong_workplace");
        }
        if (settler.getState() == SettlerState.STRIKING || settler.getState() == SettlerState.LEAVING) {
            return SettlerResult.refused("core.settler.will_not_work");
        }
        if (settler.getAssignment() == null) {
            final OptionalInt cap = site.workingCap(key, profession.getId());
            if (cap.isPresent() && roster.working(profession.getId()) >= cap.getAsInt()) {
                return SettlerResult.refused("core.settler.working_full", Component.text(cap.getAsInt()));
            }
        }

        final String previous = settler.getAssignment();
        if (Objects.equals(previous, workplace)) {
            return SettlerResult.done(settler);
        }
        settler.setAssignment(workplace);
        settler.changeState(SettlerState.WORKING, clock.getAsLong());
        site.changed(key);
        UtilServer.callEvent(new SettlerAssignedEvent(key, settler, previous));
        return SettlerResult.done(settler);
    }

    /** Takes a settler off whatever it works at, leaving it idle. */
    public @NotNull SettlerResult unassign(@NotNull SiteKey key, @NotNull UUID settlerId) {
        final SettlerSite site = sites.get(key.getSiteId());
        final Roster roster = site == null ? null : site.roster(key).orElse(null);
        final Settler settler = roster == null ? null : roster.find(settlerId).orElse(null);
        if (settler == null) {
            return SettlerResult.refused("core.settler.not_found");
        }

        final String previous = settler.getAssignment();
        if (previous == null) {
            return SettlerResult.done(settler);
        }
        settler.setAssignment(null);
        if (settler.getState() == SettlerState.WORKING) {
            settler.changeState(SettlerState.IDLE, clock.getAsLong());
        }
        site.changed(key);
        UtilServer.callEvent(new SettlerAssignedEvent(key, settler, previous));
        return SettlerResult.done(settler);
    }
}
