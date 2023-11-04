package me.mykindos.betterpvp.clans.clans.leveling.perk.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class FarmingHeightListener implements Listener {

    private final ClanPerkManager manager;

    @Inject
    public FarmingHeightListener(ClanPerkManager manager) {
        registerPerk(manager, 1, 5);
        registerPerk(manager, 2, 20);
        registerPerk(manager, 3, 25);
        registerPerk(manager, 4, 35);
        this.manager = manager;
    }

    private void registerPerk(ClanPerkManager manager, int level, int minReq) {
        ClanPerk perk = new ClanPerk() {
            @Override
            public String getName() {
                return "Farming Height " + level;
            }

            @Override
            public int getMinimumLevel() {
                return minReq;
            }

            @Override
            public Component[] getDescription() {
                return new Component[] {
                        Component.text("Increases the height at which you can farm crops by " + level + " blocks.", NamedTextColor.GRAY)
                };
            }

            @Override
            public ItemStack getIcon() {
                return new ItemStack(Material.WHEAT);
            }
        };

        manager.addObject(perk.getName(), perk);
    }

}
