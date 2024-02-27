package me.mykindos.betterpvp.clans.clans.leveling.perk.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.leveling.perk.model.ClanVaultSlot;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ClanVaultSlotsPerk implements Listener {

    @Inject
    public ClanVaultSlotsPerk(ClanPerkManager manager) {
        registerPerk(manager, 1, 5);
        registerPerk(manager, 2, 20);
        registerPerk(manager, 3, 25);
        registerPerk(manager, 4, 35);
    }

    private void registerPerk(ClanPerkManager manager, int slots, int minReq) {
        final ClanVaultSlot perk = new ClanVaultSlot(slots, minReq) {
            @Override
            public ItemView getIcon() {
                return ItemView.builder().material(Material.CHEST).build();
            }
        };
        manager.addObject(perk.getName(), perk);
    }

}
