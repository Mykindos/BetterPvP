package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.WorkplaceBonus;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.OptionalInt;

/**
 * What a camp's Farmers do for its farm: crops grow faster and harvests sometimes drop more. Each Farmer at work adds
 * a share that grows with its profession stats, Green Thumb and Bountiful, is made stronger by Seasoned and Moody, and
 * rises or falls with its morale. The whole farm stops at its caps.
 */
@Singleton
public class FarmWorkplace {

    private final SettlerConfig config;
    private final SettlerService settlers;

    @Inject
    public FarmWorkplace(@NotNull SettlerConfig config, @NotNull SettlerService settlers) {
        this.config = config;
        this.settlers = settlers;
    }

    /** The Farmers at work on the farm. */
    public @NotNull List<Settler> farmers(@NotNull SiteKey site) {
        return settlers.roster(site)
                .map(roster -> WorkplaceBonus.residents(roster, CampGrounds.FARM))
                .orElse(List.of());
    }

    /** How much faster crops grow on the farm, as a share. */
    public double growth(@NotNull SiteKey site) {
        return WorkplaceBonus.total(farmers(site),
                farmer -> share(farmer, config.farm("growth", 0.10), CampTraits.GREEN_THUMB, "growth", 0.10),
                config.farm("max-growth", 1.0));
    }

    /** The chance a harvested crop on the farm drops one more. */
    public double extraDrop(@NotNull SiteKey site) {
        return WorkplaceBonus.total(farmers(site),
                farmer -> share(farmer, config.farm("extra-drop", 0.10), CampTraits.BOUNTIFUL, "extra-drop", 0.10),
                config.farm("max-extra-drop", 0.5));
    }

    /** One Farmer's share before morale: its stats times the base, plus its own trait for this bonus, strengthened. */
    double share(@NotNull Settler farmer, double base, @NotNull String trait, @NotNull String number, double fallback) {
        final double strength = strength(farmer);
        double share = base * stats(farmer, strength);
        if (farmer.hasTrait(trait)) {
            share += config.trait(trait, number, fallback) * strength;
        }
        double stronger = 1;
        if (farmer.hasTrait(CampTraits.SEASONED)) {
            stronger += config.trait(CampTraits.SEASONED, "bonus", 0.20) * strength;
        }
        if (farmer.hasTrait(CampTraits.MOODY)) {
            stronger += config.trait(CampTraits.MOODY, "bonus", 0.30) * strength;
        }
        return share * stronger;
    }

    /** Its rarity's profession stats, with Prodigy and Homesick. */
    private double stats(@NotNull Settler farmer, double strength) {
        double stats = config.getTable().rarity(farmer.getRarity()).getStats();
        if (farmer.hasTrait(CampTraits.PRODIGY)) {
            stats *= 1 + config.trait(CampTraits.PRODIGY, "stats", 0.25) * strength;
        }
        if (farmer.hasTrait(CampTraits.HOMESICK)) {
            stats *= 1 + config.trait(CampTraits.HOMESICK, "stats", 0.10) * strength;
        }
        return stats;
    }

    private double strength(@NotNull Settler farmer) {
        return config.getTable().rarity(farmer.getRarity()).getTraitStrength();
    }

    /** How many Farmers can work the farm at once, or -1 for no limit of its own. */
    public int slots(@NotNull SiteKey site) {
        final OptionalInt cap = settlers.workingCap(site, CampProfessions.FARMER);
        return cap.isPresent() ? cap.getAsInt() : -1;
    }

    /** Farmers with nowhere to work, who could be sent to the farm. */
    public @NotNull List<Settler> idleFarmers(@NotNull SiteKey site) {
        return settlers.roster(site).map(Roster::getSettlers).orElse(List.of()).stream()
                .filter(settler -> settler.hasProfession(CampProfessions.FARMER) && settler.getAssignment() == null)
                .filter(settler -> settler.getState() == SettlerState.IDLE)
                .toList();
    }
}
