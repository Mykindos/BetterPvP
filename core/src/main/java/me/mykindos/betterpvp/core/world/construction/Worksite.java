package me.mykindos.betterpvp.core.world.construction;

import lombok.Value;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/** One site's holding in the world it stands in, and what that site's module supplies. */
@Value
public class Worksite {
    SiteKey key;
    ConstructionSite site;
    Holding holding;
    World world;

    @NotNull SiteHolding siteHolding() {
        return new SiteHolding(key, site, holding);
    }
}
