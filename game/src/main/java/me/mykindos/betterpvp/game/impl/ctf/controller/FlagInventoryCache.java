package me.mykindos.betterpvp.game.impl.ctf.controller;

import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages the caching of player inventory items
 * <p>
 * Taken from <a href="https://github.com/braulio-dev/Mapper">Mapper Plugin</a>
 */
@GameScoped
public class FlagInventoryCache {

    private final Map<Player, Map<Integer, ItemStack>> itemCache = new WeakHashMap<>();

    public boolean hasCache(Player player) {
        return itemCache.containsKey(player);
    }

    public void setInventory(Player player, Map<Integer, ItemStack> items) {
        Map<Integer, ItemStack> oldInventory = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack newItem = items.get(i);
            if (newItem == null) {
                continue;
            }

            final ItemStack oldItem = player.getInventory().getItem(i);
            if (oldItem != null) {
                oldInventory.put(i, oldItem);
            }
            player.getInventory().setItem(i, newItem);
        }
        itemCache.put(player, oldInventory);
    }

    public void refundInventory(Player player) {
        Map<Integer, ItemStack> oldInventory = itemCache.remove(player);
        if (oldInventory == null) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack oldItem = oldInventory.get(i);
            player.getInventory().setItem(i, oldItem);
        }
    }
}