package me.mykindos.betterpvp.core.menu.button;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class MenuButton<M extends Windowed> extends AbstractItem {

    private final ItemView itemView;
    private final M menu;

    public MenuButton(ItemView itemView, M menu) {
        this.itemView = itemView;
        this.menu = menu;
    }

    @Override
    public ItemProvider getItemProvider() {
        return itemView;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        menu.show(player);
    }
}
