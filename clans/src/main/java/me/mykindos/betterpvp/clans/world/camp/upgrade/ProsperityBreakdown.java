package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.CampProsperity;
import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.ProsperityFactors;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

/** Great Hall upgrade: a page listing what raises and lowers the camp's Prosperity, and by how much. */
@Singleton
public class ProsperityBreakdown {

    public static final String ID = "prosperity_breakdown";

    private final CampProsperity prosperity;

    @Inject
    public ProsperityBreakdown(@NotNull CampUpgrades upgrades, @NotNull CampProsperity prosperity) {
        this.prosperity = prosperity;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 2);
    }

    public @NotNull ProsperityFactors factors(@NotNull SiteKey key) {
        return prosperity.factors(key);
    }
}
