package me.mykindos.betterpvp.game.impl.ctf.controller;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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
    private final HotBarLayoutManager hotBarLayoutManager;
    private final BPvPItem ghostHandle = JavaPlugin.getPlugin(GamePlugin.class).getInjector().getInstance(ItemHandler.class).getItem("champions:ghost_handle");

    @Inject
    public FlagInventoryCache(HotBarLayoutManager hotBarLayoutManager) {
        this.hotBarLayoutManager = hotBarLayoutManager;
    }

    public boolean hasCache(Player player) {
        return itemCache.containsKey(player);
    }

    public void setInventory(Player player, Map<Integer, ItemStack> items) {
        final Map<Integer, ItemStack> oldInventory = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack newItem = items.get(i);
            if (newItem == null) {
                continue;
            }

            ItemStack oldItem = player.getInventory().getItem(i);
            //temp-ish fix, replace ghost handles with their respective slots
            if (ghostHandle.matches(oldItem)) {
                oldItem = hotBarLayoutManager.getPlayerHotBarLayoutSlot(player, i);
            }
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