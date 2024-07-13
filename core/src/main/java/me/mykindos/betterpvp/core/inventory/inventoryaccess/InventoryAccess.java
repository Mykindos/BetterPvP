package me.mykindos.betterpvp.core.inventory.inventoryaccess;

import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.inventory.AnvilInventory;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.inventory.CartographyInventory;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.util.InventoryUtils;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.util.ItemUtils;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.util.PlayerUtils;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.BungeeComponentWrapper;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.impl.AnvilInventoryImpl;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.impl.CartographyInventoryImpl;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.impl.InventoryUtilsImpl;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.impl.ItemUtilsImpl;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.impl.PlayerUtilsImpl;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class InventoryAccess {

    private static final InventoryUtils INVENTORY_UTILS = new InventoryUtilsImpl();
    private static final ItemUtils ITEM_UTILS = new ItemUtilsImpl();
    private static final PlayerUtils PLAYER_UTILS = new PlayerUtilsImpl();
    
    /**
     * Gets the {@link InventoryUtils}
     *
     * @return The {@link InventoryUtils}
     */
    public static InventoryUtils getInventoryUtils() {
        return INVENTORY_UTILS;
    }
    
    /**
     * Gets the {@link ItemUtils}
     *
     * @return The {@link ItemUtils}
     */
    public static ItemUtils getItemUtils() {
        return ITEM_UTILS;
    }
    
    /**
     * Gets the {@link PlayerUtils}
     *
     * @return The {@link PlayerUtils}
     */
    public static PlayerUtils getPlayerUtils() {
        return PLAYER_UTILS;
    }
    
    /**
     * Creates a new {@link AnvilInventory}.
     *
     * @param player         The {@link Player} that should see this {@link AnvilInventory}
     * @param title          The inventory title
     * @param renameHandlers A list of {@link Consumer}s that are called whenever the {@link Player}
     *                       types something in the renaming section of the anvil.
     * @return The {@link AnvilInventory}
     */
    public static AnvilInventory createAnvilInventory(@NotNull Player player, @Nullable ComponentWrapper title, @Nullable List<@NotNull Consumer<String>> renameHandlers) {
        return new AnvilInventoryImpl(player, title == null ? BungeeComponentWrapper.EMPTY : title, renameHandlers);
    }
    
    /**
     * Creates a new {@link CartographyInventory}.
     *
     * @param player The {@link Player} that should see this {@link CartographyInventory}
     * @param title  The inventory title
     * @return The {@link CartographyInventory}
     */
    public static CartographyInventory createCartographyInventory(@NotNull Player player, @Nullable ComponentWrapper title) {
        return new CartographyInventoryImpl(player, title == null ? BungeeComponentWrapper.EMPTY : title);
    }
    
}
