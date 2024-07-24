package me.mykindos.betterpvp.core.inventory.item.impl;

import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link Item} that will force a player to run a command or say something in the chat when clicked.
 */
public class CommandItem extends SimpleItem {
    
    private final String command;
    
    public CommandItem(@NotNull ItemProvider itemProvider, @NotNull String command) {
        super(itemProvider);
        this.command = command;
    }
    
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.chat(command);
    }
    
}
