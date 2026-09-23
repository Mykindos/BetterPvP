package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** Whatever pays for construction on a site. */
public interface ResourceLedger {

    boolean canAfford(@NotNull SiteKey site, @NotNull ResourceCost cost);

    void spend(@NotNull SiteKey site, @NotNull ResourceCost cost);

    void refund(@NotNull SiteKey site, @NotNull ResourceCost cost);
}
