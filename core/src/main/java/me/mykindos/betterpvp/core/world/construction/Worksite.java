package me.mykindos.betterpvp.core.world.construction;

import lombok.Value;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/** One site's holding, what that site's module supplies, and the world it stands in if that world is loaded. */
@Value
public class Worksite {
    SiteKey key;
    ConstructionSite site;
    Holding holding;
    @Nullable World world;
}
