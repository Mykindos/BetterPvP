package me.mykindos.betterpvp.clans.clans.leveling.perk.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultLegend;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultSlot;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ClanVaultPerks implements Listener {

    @Inject
    public ClanVaultPerks(ClanPerkManager manager) {
        registerSlots(manager, 1, 5);
        registerSlots(manager, 2, 20);
        registerSlots(manager, 3, 25);
        registerSlots(manager, 4, 35);

        registerLegends(manager, 1, 5);
        registerLegends(manager, 2, 20);
        registerLegends(manager, 3, 25);
        registerLegends(manager, 4, 35);
    }

    private void registerLegends(ClanPerkManager manager, int legends, int minReq) {
        final ClanVaultLegend perk = new ClanVaultLegend(legends, minReq) {
            @Override
            public ItemView getIcon() {
                return ItemView.builder().material(Material.NETHER_STAR).build();
            }
        };
        manager.addObject(perk.getName(), perk);
    }

    private void registerSlots(ClanPerkManager manager, int slots, int minReq) {
        final ClanVaultSlot perk = new ClanVaultSlot(slots, minReq) {
            @Override
            public ItemView getIcon() {
                return ItemView.builder().material(Material.CHEST).build();
            }
        };
        manager.addObject(perk.getName(), perk);
    }

}
