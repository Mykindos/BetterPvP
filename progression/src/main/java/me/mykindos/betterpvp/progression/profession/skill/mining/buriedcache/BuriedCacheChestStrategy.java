package me.mykindos.betterpvp.progression.profession.skill.mining.buriedcache;

import me.mykindos.betterpvp.core.loot.AwardStrategy;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class BuriedCacheChestStrategy {

    private BuriedCacheChestStrategy() {}

    public static void fillChest(Block block, LootBundle bundle) {
        if (!(block.getState(false) instanceof Chest chest)) return;

        final Inventory inventory = chest.getBlockInventory();
        bundle.setAwardStrategy(new AwardStrategy() {
            @Override
            public void award(LootBundle b) {
                for (Loot<?, ?> loot : b) {
                    final Object awarded = awardSingle(b, loot);
                    ItemStack itemStack = null;
                    if (awarded instanceof Item worldItem) {
                        itemStack = worldItem.getItemStack().clone();
                        worldItem.remove();
                    } else if (awarded instanceof ItemStack is) {
                        itemStack = is;
                    }

                    if (itemStack == null) continue;

                    final Map<Integer, ItemStack> overflow = inventory.addItem(itemStack);
                    for (ItemStack leftover : overflow.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), leftover);
                    }
                }
            }
        });
        bundle.award();
        chest.update();
    }
}
