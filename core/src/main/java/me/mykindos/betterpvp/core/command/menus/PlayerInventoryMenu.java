package me.mykindos.betterpvp.core.command.menus;

import lombok.Getter;
import lombok.NonNull;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory;
import me.mykindos.betterpvp.core.inventory.window.Window;
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
    private @Nullable Player player;
    @Getter
    private final ReferencingInventory playerInventory;
    private ReferencingInventory craftingInventory;
    private final String name;
    private final UUID id;
    private boolean offline;
    private final CraftInventoryPlayer craftInventoryPlayer;


    /**
     * Represents a player's inventory.
     * Can be offline or online.
     * An online player logging off will
     * set this to an offline menu.
     * An offline player logging on
     * will close this menu.
     * Changes in the primary inventory (armor + storage)
     * Will be reflected in the actual player's inventory.
     * @param player the player object, null if offline
     * @param name the name of the player
     * @param id the UUID of the player
     * @param inventoryPlayer the CraftInventoryPlayer of the player
     * @param offline whether this is an offline representation or not
     */
    public PlayerInventoryMenu(@Nullable Player player, String name, UUID id, CraftInventoryPlayer inventoryPlayer, boolean offline) {
        super(9, 6);
        this.player = player;
        this.name = name;
        this.id = id;
        this.offline = offline;
        this.craftInventoryPlayer = inventoryPlayer;
        this.playerInventory = ReferencingInventory.fromContents(inventoryPlayer);

        this.bake();

        //playerInventory.setPostUpdateHandler((itemPostUpdateEvent -> {
        //    if (!offline) return;
        //    UtilInventory.saveOfflineInventory(id, inventoryPlayer);
        //}));
    }

    private void bake() {
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

    /**
     * Called at the earliest for a player login
     * closes this menu
     * @param player the player
     */
    public void playerLogin(Player player) {
        if (!player.getUniqueId().equals(id)) return;
        //close the window, the player is logging in
        //changes will be saved, but nothing new will
        this.findAllWindows().forEach(Window::close);
    }

    /**
     * Called when a player leaves the server
     * Sets this menu to an offline menu
     * @param player the player
     */
    public void onPlayerLeave(Player player) {
        if (!player.getUniqueId().equals(id)) return;

        this.player = null;
        this.offline = true;

    }

    /**
     * Saves this inventory to file
     */
    private void saveOfflineInventory() {
        if (!offline) return;
        UtilInventory.saveOfflineInventory(id, craftInventoryPlayer);
    }


    @Override
    public @NotNull Component getTitle() {
        return Component.text(name + "'s inventory");
    }

    @Override
    public Window show(@NonNull Player player) {
        Window window = Windowed.super.show(player);
        window.addCloseHandler(this::saveOfflineInventory);
        return window;
    }
}
