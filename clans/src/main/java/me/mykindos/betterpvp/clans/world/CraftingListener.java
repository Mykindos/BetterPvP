package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler (priority = EventPriority.LOWEST)
    public void craftItem(PrepareItemCraftEvent event) {


        if (event.getRecipe() == null) return;

        ItemStack result = event.getRecipe().getResult();

        if(result.getType() == Material.AIR) return; // Fixes dyeing armour

        if (result.getType().name().contains("LEATHER_")) {

            return;
        }

        if (result.getType().name().contains("BANNER")) {
            return;
        }

        if (result.getType().name().contains("TRAPDOOR")) {
            event.getInventory().setResult(itemHandler.updateNames(new ItemStack(Material.IRON_TRAPDOOR)));
            return;
        } else if (result.getType().name().contains("_DOOR")) {
            event.getInventory().setResult(itemHandler.updateNames(new ItemStack(Material.IRON_DOOR)));
            return;
        }


        Material itemType = result.getType();
        if (itemType == Material.TNT || itemType == Material.DISPENSER
                || itemType == Material.SLIME_BLOCK || itemType == Material.COMPASS
                || itemType == Material.PISTON || itemType == Material.PISTON_HEAD || itemType == Material.ENCHANTING_TABLE
                || itemType == Material.BREWING_STAND || itemType == Material.GOLDEN_APPLE || itemType == Material.GOLDEN_CARROT
                || itemType == Material.ANVIL || itemType == Material.MAGMA_BLOCK || itemType == Material.CROSSBOW
                || itemType.name().toLowerCase().contains("boat")
                || itemType.name().contains("CAMPFIRE")
                || itemType == Material.COOKIE || itemType == Material.BEEHIVE) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        } else {

            event.getInventory().setResult(itemHandler.updateNames(result));

        }


    }

}
