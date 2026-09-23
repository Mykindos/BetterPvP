package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/**
 * Something outside a job that decides whether it may run and how fast, such as a siege stopping all work or the
 * workers on it setting its pace. Rules are asked again whenever a site's jobs are refreshed.
 */
public interface JobRule {

    /** Names the hold this rule puts on a job, so it can take that hold off again. */
    @NotNull String id();

    /** Whether the job must wait. */
    boolean holds(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job);

    /** How many times faster than its base duration the job may run. Rates from every rule multiply. */
    default double rate(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job) {
        return 1.0;
    }
}
