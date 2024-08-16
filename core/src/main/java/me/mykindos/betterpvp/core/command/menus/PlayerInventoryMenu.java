package me.mykindos.betterpvp.core.command.menus;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.CursorButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class PlayerInventoryMenu extends AbstractGui implements Windowed {
    @Getter
    private final Player player;
    @Getter
    ReferencingInventory playerInventory;
    ReferencingInventory craftingInventory;


    public PlayerInventoryMenu(Player player) {
        super(9, 6);
        this.player = player;
        this.playerInventory = ReferencingInventory.fromContents(player.getInventory());
        if (player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING) {
            craftingInventory = ReferencingInventory.fromContents(player.getOpenInventory().getTopInventory());
        }
        for (int i = 0; i < playerInventory.getSize(); i++) {
            addSlotElements(new SlotElement.InventorySlotElement(playerInventory, i) {
            });
        }

        updateInventories();

        //TODO - describe relative elements (these are unused slots)
        setItem(5, 4, Menu.BACKGROUND_GUI_ITEM);
        setItem(8, 4, Menu.BACKGROUND_GUI_ITEM);

        setItem(0, 5, Menu.BACKGROUND_GUI_ITEM);
        setItem(1, 5, Menu.BACKGROUND_GUI_ITEM);
        setItem(2, 5, Menu.BACKGROUND_GUI_ITEM);
        setItem(3, 5, Menu.BACKGROUND_GUI_ITEM);
        setItem(5, 5, Menu.BACKGROUND_GUI_ITEM);
    }

    public void updateInventories() {
        playerInventory.notifyWindows();
        if (player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING) {
            craftingInventory = ReferencingInventory.fromContents(player.getOpenInventory().getTopInventory());
            setSlotElement(8, 5, new SlotElement.InventorySlotElement(craftingInventory, 0));
            setSlotElement(6, 4, new SlotElement.InventorySlotElement(craftingInventory, 1));
            setSlotElement(7, 4, new SlotElement.InventorySlotElement(craftingInventory, 2));
            setSlotElement(6, 5, new SlotElement.InventorySlotElement(craftingInventory, 3));
            setSlotElement(7, 5, new SlotElement.InventorySlotElement(craftingInventory, 4));
            craftingInventory.notifyWindows();
        }
        setItem(4, 5, new CursorButton(player));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(player.getName() + "'s inventory");
    }

}
