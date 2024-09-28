package me.mykindos.betterpvp.core.items.menu;


import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import org.jetbrains.annotations.NotNull;

public class ItemViewMenu extends ViewCollectionMenu {
    public ItemViewMenu(@NotNull String title, @NotNull ItemHandler itemHandler, Windowed previous) {
        super(title, itemHandler.getItemButtons().stream().map(Item.class::cast).toList(), previous);
    }
}
