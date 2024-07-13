package me.mykindos.betterpvp.core.inventory.item.impl.controlitem;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import me.mykindos.betterpvp.core.inventory.gui.AbstractTabGui;
import me.mykindos.betterpvp.core.inventory.gui.TabGui;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;

/**
 * Switches between tabs in a {@link AbstractTabGui}
 */
public abstract class TabItem extends ControlItem<TabGui> {
    
    private final int tab;
    
    public TabItem(int tab) {
        this.tab = tab;
    }
    
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType == ClickType.LEFT) getGui().setTab(tab);
    }
    
}
