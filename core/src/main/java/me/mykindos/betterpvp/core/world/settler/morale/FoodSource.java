package me.mykindos.betterpvp.core.world.settler.morale;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/**
 * Something that feeds a site's settlers, such as a granary. Food only ever raises morale, so a site with none is not
 * punished for it.
 */
public interface FoodSource {

    /** How much morale food gives every settler at {@code site} now. 0 when there is none. */
    int morale(@NotNull SiteKey site);
}
