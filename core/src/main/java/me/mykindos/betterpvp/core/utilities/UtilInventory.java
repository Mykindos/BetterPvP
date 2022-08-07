package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class UtilInventory {

    public static boolean contains(Player player, Material item, int required) {

        for (int i : player.getInventory().all(item).keySet()) {
            if (required <= 0) {
                return true;
            }

            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getAmount() > 0) {
                required -= stack.getAmount();
            }
        }

        return required <= 0;
    }

    public static boolean remove(Player player, Material item, int toRemove) {
        if (contains(player, item, toRemove)) {
            for (int i = 0; i < player.getInventory().getSize(); ++i) {
                if (player.getInventory().getItem(i) != null) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack == null) continue;
                    if (stack.getType() == item) {
                        if (stack.getAmount() > toRemove) {
                            stack.setAmount(stack.getAmount() - toRemove);
                            player.updateInventory();
                        } else {
                            player.getInventory().setItem(i, (ItemStack) null);
                            player.updateInventory();
                        }

                        return true;
                    }
                }
            }

        }
        return false;
    }

}
