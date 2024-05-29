package me.mykindos.betterpvp.clans.clans.leveling;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanFarmingLevels;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultLegend;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultSlot;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Singleton
public class ClanPerkManager extends Manager<ClanPerk> {

    private static ClanPerkManager instance;

    private ClanPerkManager() {
    }

    public static ClanPerkManager getInstance() {
        if (instance == null) {
            instance = new ClanPerkManager();
        }
        return instance;
    }

    public void init() {
        registerPerks();
    }

    private void registerPerks() {
        registerSlots(1, 5);
        registerSlots(2, 15);
        registerSlots(3, 25);
        registerSlots(4, 35);

        registerLegends(1, 15);
        registerLegends(2, 35);
        registerLegends(3, 55);
        registerLegends(4, 75);
        registerLegends(5, 95);

        registerFarmingLevels(5, 10);
        registerFarmingLevels(5, 30);
        registerFarmingLevels(5, 50);
        registerFarmingLevels(5, 70);
        registerFarmingLevels(5, 90);
    }

    public Collection<ClanPerk> getPerks(Clan clan) {
        return objects.values().stream().filter(perk -> perk.getMinimumLevel() <= clan.getLevel()).toList();
    }

    public List<ClanPerk> getPerksSortedByLevel() {
        return objects.values().stream().sorted(Comparator.comparingInt(ClanPerk::getMinimumLevel)).toList();
    }

    public boolean hasPerk(Clan clan, ClanPerk perk) {
        return getPerks(clan).contains(perk);
    }

    public boolean hasPerk(Clan clan, Class<?> perk) {
        return getPerks(clan).stream().anyMatch(perk::isInstance);
    }

    private void registerLegends(int legends, int minReq) {
        final ClanVaultLegend perk = new ClanVaultLegend(legends, minReq);
        addObject(perk.getName(), perk);
    }

    private void registerSlots(int slots, int minReq) {
        final ClanVaultSlot perk = new ClanVaultSlot(slots, minReq);
        addObject(perk.getName(), perk);
    }

    private void registerFarmingLevels(int levels, int minReq) {
        final ClanFarmingLevels perk = new ClanFarmingLevels(levels, minReq);
        addObject(perk.getPerkUUID(), perk);
    }

    public int getTotalFarmingLevels(Clan clan) {
        return getPerks(clan).stream().filter(ClanFarmingLevels.class::isInstance).mapToInt(perk -> ((ClanFarmingLevels) perk).getLevels()).sum();
    }
}
