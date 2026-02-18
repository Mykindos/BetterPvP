package me.mykindos.betterpvp.clans.clans.leveling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanFarmingLevels;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultSlot;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class ClanPerkManager extends Manager<String, ClanPerk> {

    // Static accessor retained so non-injected callers (UI, vault, etc.) can reach the singleton.
    private static ClanPerkManager instance;

    @Inject
    public ClanPerkManager(ClanExperience formula) {
        instance = this;
    }

    /**
     * Static accessor for non-injected callers (menus, vault size checks, etc.).
     * The instance is always set before any listener or menu code runs.
     */
    public static ClanPerkManager getInstance() {
        return instance;
    }

    public void init() {
        registerPerks();
    }

    private void registerPerks() {
        addObject("vault_slot_5",   new ClanVaultSlot("vault_slot_5",   1, 5));
        addObject("vault_slot_15",  new ClanVaultSlot("vault_slot_15",  2, 15));
        addObject("vault_slot_25",  new ClanVaultSlot("vault_slot_25",  3, 25));
        addObject("vault_slot_35",  new ClanVaultSlot("vault_slot_35",  3, 35));
        addObject("vault_slot_50",  new ClanVaultSlot("vault_slot_50",  3, 50));
        addObject("vault_slot_65",  new ClanVaultSlot("vault_slot_65",  3, 65));
        addObject("vault_slot_80",  new ClanVaultSlot("vault_slot_80",  4, 80));

        //addObject("vault_legend_15", new ClanVaultLegend("vault_legend_15", 1, 15));
        //addObject("vault_legend_35", new ClanVaultLegend("vault_legend_35", 2, 35));
        //addObject("vault_legend_55", new ClanVaultLegend("vault_legend_55", 3, 55));
        //addObject("vault_legend_75", new ClanVaultLegend("vault_legend_75", 4, 75));
        //addObject("vault_legend_95", new ClanVaultLegend("vault_legend_95", 5, 95));

        addObject("farming_10", new ClanFarmingLevels("farming_10", 5, 10));
        addObject("farming_30", new ClanFarmingLevels("farming_30", 5, 30));
        addObject("farming_50", new ClanFarmingLevels("farming_50", 5, 50));
        addObject("farming_70", new ClanFarmingLevels("farming_70", 5, 70));
        addObject("farming_90", new ClanFarmingLevels("farming_90", 5, 90));
    }

    public Collection<ClanPerk> getPerks(Clan clan) {
        long level = clan.getExperience().getLevel();
        return objects.values().stream().filter(perk -> perk.hasPerk(level)).toList();
    }

    public List<ClanPerk> getPerksSortedByLevel() {
        return objects.values().stream().sorted(Comparator.comparingInt(ClanPerk::getMinimumLevel)).toList();
    }

    public boolean hasPerk(Clan clan, ClanPerk perk) {
        return getPerks(clan).contains(perk);
    }

    public boolean hasPerk(Clan clan, Class<?> perkType) {
        return getPerks(clan).stream().anyMatch(perkType::isInstance);
    }

    public int getTotalFarmingLevels(Clan clan) {
        return getPerks(clan).stream()
                .filter(ClanFarmingLevels.class::isInstance)
                .mapToInt(perk -> ((ClanFarmingLevels) perk).getLevels())
                .sum();
    }

    /**
     * Returns all perks grouped by category, sorted by minimum level within each group.
     */
    public Map<ClanPerkCategory, List<ClanPerk>> getPerksByCategory() {
        return objects.values().stream()
                .sorted(Comparator.comparingInt(ClanPerk::getMinimumLevel))
                .collect(Collectors.groupingBy(ClanPerk::getCategory));
    }

}
