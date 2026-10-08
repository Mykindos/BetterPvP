package me.mykindos.betterpvp.core.world.construction;

import lombok.Value;
import me.mykindos.betterpvp.core.world.site.SiteKey;

/** One site's loaded holding and its {@link ConstructionSite}, for checks that work whether its world is loaded or not. */
@Value
class SiteHolding {
    SiteKey key;
    ConstructionSite site;
    Holding holding;
}
