package me.mykindos.betterpvp.core.command.menus;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.CursorButton;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerInventoryMenu extends AbstractGui implements Windowed {
    @Getter
    private final @Nullable Player player;
    @Getter
    private ReferencingInventory playerInventory;
    private ReferencingInventory craftingInventory;
    private String name;
    private UUID id;
    private boolean offline;


    /**
     * TODO
     * @param player
     * @param name
     * @param id
     * @param inventoryPlayer
     * @param offline
     */
    public PlayerInventoryMenu(@Nullable Player player, String name, UUID id, CraftInventoryPlayer inventoryPlayer, boolean offline) {
        super(9, 6);
        this.player = player;
        this.name = name;
        this.id = id;
        this.offline = offline;
        this.playerInventory = ReferencingInventory.fromContents(inventoryPlayer);
        if (player != null && player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING) {
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

        playerInventory.setPostUpdateHandler((itemPostUpdateEvent -> {
            if (!offline) return;
            UtilInventory.saveOfflineInventory(id, inventoryPlayer);
        }));
    }

    public void updateInventories() {
        playerInventory.notifyWindows();
        if (player != null && player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING) {
            craftingInventory = ReferencingInventory.fromContents(player.getOpenInventory().getTopInventory());
            setSlotElement(8, 5, new SlotElement.InventorySlotElement(craftingInventory, 0));
            setSlotElement(6, 4, new SlotElement.InventorySlotElement(craftingInventory, 1));
            setSlotElement(7, 4, new SlotElement.InventorySlotElement(craftingInventory, 2));
            setSlotElement(6, 5, new SlotElement.InventorySlotElement(craftingInventory, 3));
            setSlotElement(7, 5, new SlotElement.InventorySlotElement(craftingInventory, 4));
            craftingInventory.notifyWindows();
        }
        if (player != null) {
            setItem(4, 5, new CursorButton(player));
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(name + "'s inventory");
    }

}
