package me.mykindos.betterpvp.game.impl.ctf.controller;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static me.mykindos.betterpvp.core.utilities.Resources.ItemModel.INVISIBLE;

/**
 * Manages the caching of player inventory items
 * <p>
 * Taken from <a href="https://github.com/braulio-dev/Mapper">Mapper Plugin</a>
 */
@GameScoped
public class FlagInventoryCache {

    private final Map<Player, Map<Integer, ItemStack>> itemCache = new WeakHashMap<>();
    private final HotBarLayoutManager hotBarLayoutManager;
    private final ItemFactory itemFactory;
    private final BaseItem placeholderItem;

    @Inject
    public FlagInventoryCache(GamePlugin plugin, HotBarLayoutManager hotBarLayoutManager, ItemRegistry registry, ItemFactory itemFactory) {
        this.hotBarLayoutManager = hotBarLayoutManager;
        this.itemFactory = itemFactory;
        this.placeholderItem = new BaseItem("Flag Placeholder",
                ItemView.builder().material(Material.STICK).itemModel(INVISIBLE).hideTooltip(true).build().get(),
                ItemGroup.MISC,
                ItemRarity.COMMON);
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
            if (oldItem != null && itemFactory.isItemOfType(oldItem, placeholderItem)) {
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