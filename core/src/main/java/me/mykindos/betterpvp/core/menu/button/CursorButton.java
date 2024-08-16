package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CursorButton extends AbstractItem {
    private final Player target;
    public CursorButton(Player player) {
        this.target = player;
    }
    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider() {
        if (target.getItemOnCursor().getItemMeta() != null) {
            return ItemView.of(target.getItemOnCursor());
        }
        return ItemProvider.EMPTY;
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
        ItemStack newItem = player.getItemOnCursor();
        player.setItemOnCursor(target.getItemOnCursor());
        target.setItemOnCursor(newItem);
        notifyWindows();
    }
}
