package me.mykindos.betterpvp.core.world.settler;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Everything settlers need from the module that owns a kind of site: where its rosters are kept and how many settlers
 * each can hold. Registered per site id with the {@link SettlerService}, so core never learns what the owner of a site
 * is.
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
}
