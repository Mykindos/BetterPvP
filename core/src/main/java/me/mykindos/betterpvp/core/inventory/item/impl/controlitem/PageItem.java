package me.mykindos.betterpvp.core.inventory.item.impl.controlitem;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.PagedGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Switches between pages in a {@link AbstractPagedGui}
 */
public abstract class PageItem extends ControlItem<PagedGui<?>> {
    
    private final boolean forward;
    
    public PageItem(boolean forward) {
        this.forward = forward;
    }
    
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType == ClickType.LEFT) {
            if (forward) getGui().goForward();
            else getGui().goBack();
        }
    }
    
}
