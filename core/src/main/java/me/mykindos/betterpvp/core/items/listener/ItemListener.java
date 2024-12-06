package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.events.CustomPlayerItemEvent;
import me.mykindos.betterpvp.core.items.events.ItemStatus;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

@BPvPListener
public class ItemListener implements Listener {

    private final ItemHandler itemHandler;

    private final Map<UUID, Inventory> lastInventory = new WeakHashMap<>();
    private final Map<UUID, ItemStack> lastHeldItem = new WeakHashMap<>();

    private static final List<InventoryType> INVENTORY_NO_STORE_TYPES = new ArrayList<>(List.of(
            InventoryType.ANVIL,
            InventoryType.WORKBENCH,
            InventoryType.CRAFTING
    ));

    @Inject
    public ItemListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onImmuneDamage(PlayerItemDamageEvent event) {
        BPvPItem item = itemHandler.getItem(event.getItem());
        if (item != null && !item.hasDurability()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageItem(PlayerItemDamageEvent event) {
        itemHandler.updateNames(event.getItem());
    }


    //begin customitemevents

    //Lose
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();
        UtilServer.callEvent(
                new CustomPlayerItemEvent(
                        event.getPlayer(),
                        event.getItemDrop().getItemStack(),
                        itemHandler.getItem(itemStack),
                        event.getPlayer().getInventory(),
                        null,
                        ItemStatus.LOSE
                )
        );
    }

    /**
     * Tracks when an item is placed in an inventory.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("PLACE") && event.getClickedInventory() != null) {
                placeItemLogic(player, Objects.requireNonNull(event.getClickedInventory()), event.getCursor());
            }
        }
    }

    //Gain
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack itemStack = event.getItem().getItemStack();
        UtilServer.callEvent(
                new CustomPlayerItemEvent(
                        player,
                        itemStack,
                        itemHandler.getItem(itemStack),
                        null,
                        player.getInventory(),
                        ItemStatus.GAIN
                )
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCloseInventory(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (lastHeldItem.containsKey(player.getUniqueId()) && lastInventory.containsKey(player.getUniqueId())) {
                //the player is holding an item, and the inventory has closed. This means they have the item.
                ItemStack item = lastHeldItem.get(player.getUniqueId());
                Inventory inventory = lastInventory.get(player.getUniqueId());
                if (Objects.equals(inventory.getHolder(), player) && inventory.getType().equals(InventoryType.PLAYER)) {
                    //The last inventory is the player's, so not actually retrieving
                    lastHeldItem.remove(player.getUniqueId());
                    lastInventory.remove(player.getUniqueId());
                    return;
                }
                Location location = inventory.getLocation();
                assert location != null;
                UtilServer.callEvent(
                        new CustomPlayerItemEvent(
                                player,
                                item,
                                itemHandler.getItem(item),
                                inventory,
                                player.getInventory(),
                                ItemStatus.GAIN
                        )
                );
                lastHeldItem.remove(player.getUniqueId());
                lastInventory.remove(player.getUniqueId());
            }
        }
    }

    //Utility
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventorySwapWithCursor(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
                //cannot place an UUIDItem in a fuel slot
                placeItemLogic(player, Objects.requireNonNull(event.getClickedInventory()), event.getCursor());
                //now do pickup logic
                lastInventory.put(player.getUniqueId(), event.getClickedInventory());
                lastHeldItem.put(player.getUniqueId(), event.getCurrentItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryHotbar(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("HOTBAR")) {
                if (Objects.requireNonNull(event.getClickedInventory()).getHolder() != null && !event.getClickedInventory().getHolder().equals(player)) {
                    processRetrieveItem(player, event.getClickedInventory(), event.getCurrentItem());
                    UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), false, () -> processStoreItemInSlot(player, event.getClickedInventory(), event.getSlot()), 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMoveToOtherInventory(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                Inventory inventory = event.getClickedInventory();
                assert inventory != null;
                if (inventory.getType().equals(InventoryType.PLAYER) && inventory.getHolder().equals(player)) {
                    processStoreItem(player, event.getInventory(), event.getCurrentItem());
                } else {
                    processRetrieveItem(player, inventory, event.getCurrentItem());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickup(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("PICKUP")) {
                lastInventory.put(player.getUniqueId(), event.getClickedInventory());
                lastHeldItem.put(player.getUniqueId(), event.getCurrentItem());
            }
        }
    }


    private void placeItemLogic(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //This is an inventory that can store items
            if (lastInventory.containsKey(player.getUniqueId())) {
                if (lastInventory.get(player.getUniqueId()) != inventory) {
                    if (inventory.getType().equals(InventoryType.PLAYER) && inventory.getHolder().equals(player)) {
                        processRetrieveItem(player, lastInventory.get(player.getUniqueId()), itemStack);
                    } else {
                        processStoreItem(player, inventory, itemStack);
                    }
                }
            }
            lastInventory.remove(player.getUniqueId());
            lastHeldItem.remove(player.getUniqueId());
        }

    }

    private void processRetrieveItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (itemStack.getType().equals(Material.AIR)) return;
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items, therefore we can retrieve from it
            UtilServer.callEvent(
                    new CustomPlayerItemEvent(
                            player,
                            itemStack,
                            itemHandler.getItem(itemStack),
                            inventory,
                            player.getInventory(),
                            ItemStatus.GAIN
                    )
            );
        }
    }

    private void processStoreItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (itemStack.getType().equals(Material.AIR)) return;
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items
            UtilServer.callEvent(
                    new CustomPlayerItemEvent(
                            player,
                            itemStack,
                            itemHandler.getItem(itemStack),
                            player.getInventory(),
                            inventory,
                            ItemStatus.LOSE
                    )
            );
        }
    }

    private void processStoreItemInSlot(Player player, Inventory inventory, int slot) {
        processStoreItem(player, inventory, inventory.getItem(slot));
    }

}
