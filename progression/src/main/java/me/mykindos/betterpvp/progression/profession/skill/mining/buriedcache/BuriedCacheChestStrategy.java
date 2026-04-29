package me.mykindos.betterpvp.progression.profession.skill.mining.buriedcache;

import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class BuriedCacheChestStrategy {

    private BuriedCacheChestStrategy() {}

    public static void fillChest(Block block, LootBundle bundle, LootContext context) {
        if (!(block.getState() instanceof Chest chest)) return;

        Inventory inventory = chest.getInventory();
        for (Loot<?, ?> loot : bundle) {
            Object awarded = loot.award(context);
            ItemStack itemStack = null;
            if (awarded instanceof Item worldItem) {
                itemStack = worldItem.getItemStack().clone();
                worldItem.remove();
            } else if (awarded instanceof ItemStack is) {
                itemStack = is;
            }

            if (itemStack == null) continue;

            Map<Integer, ItemStack> overflow = inventory.addItem(itemStack);
            for (ItemStack leftover : overflow.values()) {
                block.getWorld().dropItemNaturally(block.getLocation(), leftover);
            }
        }
        chest.update();
    }
}
