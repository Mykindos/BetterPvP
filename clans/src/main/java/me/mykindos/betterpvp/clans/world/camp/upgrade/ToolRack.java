package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/** Workshop upgrade: a Laborer counts as compatible with every trade, and every trade with a Laborer. */
@Singleton
public class ToolRack {

    public static final String ID = "tool_rack";

    private static final Set<String> TRADES = Set.of(CampProfessions.MASON, CampProfessions.CARPENTER,
            CampProfessions.SMITH, CampProfessions.LABORER);

    private final CampUpgrades upgrades;

    @Inject
    public ToolRack(@NotNull CampUpgrades upgrades) {
        this.upgrades = upgrades;
        upgrades.declare(CampStructures.WORKSHOP, ID, 1);
    }

    /** {@code stats} with the rack's compatibility added, if camp {@code key} has it. */
    public @NotNull BuilderStats apply(@NotNull SiteKey key, @NotNull BuilderStats stats) {
        if (!upgrades.has(key, CampStructures.WORKSHOP, ID)) {
            return stats;
        }
        final Set<String> compatible = new HashSet<>(stats.getCompatible());
        if (CampProfessions.LABORER.equals(stats.getTrade())) {
            compatible.addAll(TRADES);
            compatible.remove(CampProfessions.LABORER);
        } else {
            compatible.add(CampProfessions.LABORER);
        }
        return new BuilderStats(stats.getWorkforce(), stats.getSpeed(), stats.getEfficiency(), stats.getTrade(),
                Set.copyOf(compatible));
    }
}
