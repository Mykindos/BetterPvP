package me.mykindos.betterpvp.core.command.menus;

import lombok.CustomLog;
import lombok.Getter;
import lombok.NonNull;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.ItemPostUpdateEvent;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
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

@CustomLog
public class PlayerInventoryMenu extends AbstractGui implements Windowed {

    private final ItemRegistry registry;
    private final ComponentLookupService lookupService;

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
    public PlayerInventoryMenu(ItemRegistry registry, ComponentLookupService lookupService, @Nullable Player player, String name, UUID id, CraftInventoryPlayer inventoryPlayer, boolean offline) {
        super(9, 6);
        this.registry = registry;
        this.lookupService = lookupService;
        this.player = player;
        this.name = name;
        this.id = id;
        this.offline = offline;
        this.craftInventoryPlayer = inventoryPlayer;
        this.playerInventory = ReferencingInventory.fromContents(inventoryPlayer);

        playerInventory.setPostUpdateHandler(this::onPostItemUpdate);
        this.bake();

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


    private void onPostItemUpdate(ItemPostUpdateEvent event) {

        //there should only be one viewer
        if (this.findAllCurrentViewers().size() > 1) {
            log.warn("Multiple Viewers for this GUI {} when 1 is expected",
                    this.getClass().getName()).submit();
            return;
        }
        Player viewer = this.findAllCurrentViewers().stream().findFirst().orElseThrow();

        // Player is getting the new UUIDItem
        lookupService.getItemComponentPair(event.getNewItem(), UUIDProperty.class).ifPresent(result -> {
            final UUIDProperty component = result.getComponent();
            final ItemInstance item = result.getItem();
            log.info("{} put ({}) in {}'s inventory", viewer.getName(), component.getUniqueId().toString(), player.getName())
                    .setAction("ITEM_INVSEE_PUT").addClientContext(player)
                    .addItemContext(registry, item)
                    .addClientContext(viewer)
                    .addClientContext(player, true)
                    .submit();
        });

        // Viewer is getting the new UUIDItem
        lookupService.getItemComponentPair(event.getPreviousItem(), UUIDProperty.class).ifPresent(result -> {
            final UUIDProperty component = result.getComponent();
            final ItemInstance item = result.getItem();
            log.info("{} retrieved ({}) from {}'s inventory", viewer.getName(), component.getUniqueId().toString(), player.getName())
                    .setAction("ITEM_INVSEE_RETRIEVE").addClientContext(player)
                    .addItemContext(registry, item)
                    .addClientContext(viewer)
                    .addClientContext(player, true)
                    .submit();
        });
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
