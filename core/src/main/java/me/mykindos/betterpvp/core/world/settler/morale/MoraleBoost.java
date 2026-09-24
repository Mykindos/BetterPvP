package me.mykindos.betterpvp.core.world.settler.morale;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/**
 * Something other than food that lifts every settler at a site for a while. Where only the best food counts, every
 * boost adds to the others.
 */
public interface MoraleBoost {

    /** How much morale this gives every settler at {@code site} now. 0 when it gives none. */
    int morale(@NotNull SiteKey site);
}
