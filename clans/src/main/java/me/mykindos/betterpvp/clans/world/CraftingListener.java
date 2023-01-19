package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

@BPvPListener
public class CraftingListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public CraftingListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void craftItem(PrepareItemCraftEvent e) {

        if (e.getRecipe() != null) {
            e.getRecipe().getResult();

            if (e.getRecipe().getResult().getType().name().contains("LEATHER_")) {
                return;
            }

            if (e.getRecipe().getResult().getType().name().contains("BANNER")) {
                return;
            }

            // TODO try overriding recipes instead
            if(e.getRecipe().getResult().getType().name().contains("TRAPDOOR")){
                e.getInventory().setResult(itemHandler.updateNames(new ItemStack(Material.IRON_TRAPDOOR)));
                return;
            }else if(e.getRecipe().getResult().getType().name().contains("_DOOR")){
                e.getInventory().setResult(itemHandler.updateNames(new ItemStack(Material.IRON_DOOR)));
                return;
            }

            // TODO load from database
            Material itemType = e.getRecipe().getResult().getType();
            if (itemType == Material.TNT || itemType == Material.DISPENSER
                    || itemType == Material.SLIME_BLOCK || itemType == Material.COMPASS
                    || itemType == Material.PISTON || itemType == Material.PISTON_HEAD || itemType == Material.ENCHANTING_TABLE
                    || itemType.name().contains("_PANE")
                    || itemType == Material.BREWING_STAND || itemType == Material.GOLDEN_APPLE || itemType == Material.GOLDEN_CARROT
                    || itemType == Material.ANVIL || itemType == Material.MAGMA_BLOCK || itemType == Material.CROSSBOW
                    || itemType.name().toLowerCase().contains("boat")
                    || itemType.name().contains("CAMPFIRE")
                    || itemType == Material.COOKIE || itemType == Material.BEEHIVE) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            } else {
                if (e.getRecipe() != null) {
                    e.getInventory().setResult(itemHandler.updateNames(e.getRecipe().getResult()));
                }
            }

        }


    }

}
