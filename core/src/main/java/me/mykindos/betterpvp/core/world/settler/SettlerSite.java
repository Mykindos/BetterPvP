package me.mykindos.betterpvp.core.world.settler;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.settler.crew.CrewLimits;
import me.mykindos.betterpvp.core.world.settler.wage.CoinAccount;
import me.mykindos.betterpvp.core.world.settler.wage.WageModel;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Everything settlers need from the module that owns a kind of site: where its rosters are kept, how many settlers
 * each can hold, how they look, where in its world they gather and work, what its Builders bring to a job and how
 * its settlers are paid. Registered per site id with the {@link SettlerService}, so core never learns what the owner
 * of a site is.
 */
public interface SettlerSite {

    /** The roster for {@code site}, or empty if its record is not loaded. */
    @NotNull Optional<Roster> roster(@NotNull SiteKey site);

    /** Called after every change to a roster, so its record can be written down. */
    void changed(@NotNull SiteKey site);

    /** How many settlers {@code site} can have in all. */
    int populationCap(@NotNull SiteKey site);

    /** How many settlers of {@code profession} can work at once, or empty when only the population cap applies. */
    @NotNull OptionalInt workingCap(@NotNull SiteKey site, @NotNull String profession);

    /** Whether {@code player} may take {@code action} on the settlers of {@code site}. */
    boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull SettlerAction action);

    @NotNull SettlerLook look(@NotNull SiteKey site, @NotNull Settler settler);

    /** Where settlers of {@code site} appear and wander around when they have nowhere to be. */
    default @NotNull Optional<Location> home(@NotNull SiteKey site, @NotNull World world, @NotNull RegionIndex regions) {
        return Optional.empty();
    }

    /** Where a settler assigned to {@code workplace} stands to work, or empty if it cannot be found here. */
    default @NotNull Optional<Location> workplace(@NotNull SiteKey site, @NotNull World world,
                                                  @NotNull RegionIndex regions, @NotNull String workplace) {
        return Optional.empty();
    }

    /** What happens when {@code player} right-clicks a settler. */
    default void interact(@NotNull Player player, @NotNull SiteKey site, @NotNull Settler settler) {
    }

    /**
     * What {@code settler} brings to {@code job} while working beside {@code crew}, which includes it, or empty if it
     * cannot build.
     */
    default @NotNull Optional<BuilderStats> builderStats(@NotNull SiteKey site, @NotNull Settler settler,
                                                         @NotNull PlacedStructure structure, @NotNull Job job,
                                                         @NotNull List<Settler> crew) {
        return Optional.empty();
    }

    default @NotNull CrewLimits crewLimits(@NotNull SiteKey site) {
        return CrewLimits.NONE;
    }

    /** Called once when a job its crew worked on finishes, before the crew is let go. */
    default void crewFinished(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job,
                              @NotNull List<Settler> crew) {
    }

    /** How the site's settlers are paid, or empty if they are not. */
    default @NotNull Optional<WageModel> wageModel(@NotNull SiteKey site) {
        return Optional.empty();
    }

    /** Where the site's wages come from, or empty if they are not paid. */
    default @NotNull Optional<CoinAccount> wageFund(@NotNull SiteKey site) {
        return Optional.empty();
    }

    /** What {@code settler}'s wage is multiplied by, such as for a trait that asks for more. */
    default double wageMultiplier(@NotNull SiteKey site, @NotNull Settler settler) {
        return 1.0;
    }

    /** How long a settler strikes for before it leaves for good. */
    default @NotNull Duration strikeLimit(@NotNull SiteKey site) {
        return Duration.ofHours(72);
    }
}
