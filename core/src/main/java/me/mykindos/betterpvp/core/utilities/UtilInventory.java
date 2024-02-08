package me.mykindos.betterpvp.core.utilities;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;


public class UtilInventory {

    public static boolean isPlayerInventory(Player player, int containerId) {
        return containerId == -2 || ((CraftPlayer) player).getHandle().inventoryMenu.containerId == containerId;
    }

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

    private static boolean removeFromHand(Player player, ItemStack hand, Material item, int toRemove) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (hand.getType() == item) {
            if (hand.getAmount() > toRemove) {
                hand.setAmount(hand.getAmount() - toRemove);
            } else {
                hand.setAmount(0);
            }
            return true;
        }
        return false;
    }

    public static boolean remove(Player player, Material item, int toRemove) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        if (removeFromHand(player, player.getInventory().getItemInMainHand(), item, toRemove)) return true;
        if (removeFromHand(player, player.getInventory().getItemInOffHand(), item, toRemove)) return true;

        if (contains(player, item, toRemove)) {
            Map<Integer, ? extends ItemStack> allItems = player.getInventory().all(item);
            for (Map.Entry<Integer, ? extends ItemStack> entry : allItems.entrySet()) {
                ItemStack stack = entry.getValue();
                if (stack.getAmount() > toRemove) {
                    stack.setAmount(stack.getAmount() - toRemove);
                    return true;
                } else {
                    player.getInventory().setItem(entry.getKey(), null);
                    toRemove -= stack.getAmount();
                }
            }
        }
        return false;
    }

}
