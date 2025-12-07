package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.recipe.crafting.menu.GuiCraftingTable;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.CraftingInventory;

@Singleton
@BPvPListener
public class CraftingTableListener implements Listener {

    private final Core core;

    @Inject
    private CraftingTableListener(Core core) {
        this.core = core;
    }

    /**
     * Intercepts vanilla crafting table opens and replaces with custom GUI
     */
    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getView().getTopInventory() instanceof CraftingInventory)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);
        final GuiCraftingTable gui = core.getInjector().getInstance(GuiCraftingTable.class);
        gui.show(player);
    }

}

