package me.mykindos.betterpvp.core.items.menu;

import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ItemButton extends AbstractItem {
    private final BPvPItem bPvPItem;
    private final ItemProvider itemProvider;

    public ItemButton(BPvPItem bPvPItem, ItemProvider itemProvider) {
        this.bPvPItem = bPvPItem;
        this.itemProvider = itemProvider;
    }

    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider() {
        return itemProvider;
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        bPvPItem.clickFunction(new Click(event));
    }
}
