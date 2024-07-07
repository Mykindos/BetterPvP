package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static boolean contains(Player player, Material item, int required, int customModelData) {
        for (int i : player.getInventory().all(item).keySet()) {
            if (required <= 0) {
                return true;
            }

            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getAmount() > 0) {
                ItemMeta itemMeta = stack.getItemMeta();
                if (itemMeta == null) continue;
                if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == customModelData) {
                    required -= stack.getAmount();
                }

            }
        }

        return required <= 0;
    }

    public static boolean contains(Player player, String namespacedKey, int required) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null) continue;

            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (pdc.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING)) {
                String key = pdc.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
                if (key != null) {
                    if (key.equalsIgnoreCase(namespacedKey)) {
                        count += item.getAmount();
                    }
                }
            }

        }

        return count >= required || player.getGameMode() == GameMode.CREATIVE;
    }

    public static void remove(Player player, String namespacedKey, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        int amountToRemove = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta == null) continue;

            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (pdc.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING)) {
                String key = pdc.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
                if (key != null) {
                    if (key.equalsIgnoreCase(namespacedKey)) {
                        if (item.getAmount() > amountToRemove) {
                            item.setAmount(item.getAmount() - amountToRemove);
                            return;
                        } else {
                            amountToRemove -= item.getAmount();
                            item.setAmount(0);
                        }
                    }
                }
            }

            if (amountToRemove <= 0) return;
        }
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

    public static int getCount(ItemStack[] contents, Predicate<ItemStack> matches) {
        int count = 0;
        for (ItemStack item : contents) {
            if (item != null && matches.test(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
