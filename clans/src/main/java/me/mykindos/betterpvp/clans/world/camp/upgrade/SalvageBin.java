package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** Workshop upgrade: demolishing a structure gives back at least its configured share of what it cost. */
@Singleton
public class SalvageBin {

    public static final String ID = "salvage_bin";

    private final CampUpgrades upgrades;
    private final CampConfig config;

    @Inject
    public SalvageBin(@NotNull CampUpgrades upgrades, @NotNull CampConfig config) {
        this.upgrades = upgrades;
        this.config = config;
        upgrades.declare(CampStructures.WORKSHOP, ID, 1);
    }

    /** The share demolishing gives back in camp {@code key}, where the structure alone would give {@code share}. */
    public double refund(@NotNull SiteKey key, double share) {
        if (!upgrades.has(key, CampStructures.WORKSHOP, ID)) {
            return share;
        }
        final double salvaged = config.upgrade(CampStructures.WORKSHOP, ID)
                .map(numbers -> numbers.setting("refund", 0.5))
                .orElse(0.5);
        return Math.max(share, salvaged);
    }
}
