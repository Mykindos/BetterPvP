package me.mykindos.betterpvp.core.world.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WorldLogListener implements Listener {

    /**
     * A list of inventories that do not store items. I.e. a crafting table or anvil
     */
    private static final List<InventoryType> INVENTORY_NO_STORE_TYPES = new ArrayList<>(List.of(
            InventoryType.ANVIL,
            InventoryType.WORKBENCH,
            InventoryType.CRAFTING
    ));

    private static final List<InventoryType> INVENTORY_FURNACE_TYPES = new ArrayList<>(List.of(
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER
    ));

    private final Map<Player, Inventory> lastInventory = new WeakHashMap<>();
    private final Map<Player, ItemStack> lastHeldItem = new WeakHashMap<>();

    private final Core core;
    private final WorldLogHandler worldLogHandler;

    @Inject
    public WorldLogListener(Core core, WorldLogHandler worldLogHandler) {
        this.core = core;
        this.worldLogHandler = worldLogHandler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInspectPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!worldLogHandler.getInspectingPlayers().contains(player.getUniqueId())) return;

        event.setCancelled(true);

        // Lookups
        WorldLogSession session = worldLogHandler.getSession(player.getUniqueId());
        session.setStatement(worldLogHandler.getWorldLogRepository().getStatementForBlock(event.getBlock()));

        worldLogHandler.displayResults(player, session, 1);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInspectBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!worldLogHandler.getInspectingPlayers().contains(player.getUniqueId())) return;

        event.setCancelled(true);

        Block block = event.getBlock();
        if(block.getState() instanceof DoubleChest doubleChest) {
            block = doubleChest.getLocation().getBlock();
        }

        // Lookups
        WorldLogSession session = worldLogHandler.getSession(player.getUniqueId());
        session.setStatement(worldLogHandler.getWorldLogRepository().getStatementForBlock(block));

        worldLogHandler.displayResults(player, session, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakBlock(BlockBreakEvent event) {
        WorldLog worldLog = WorldLog.builder()
                .block(event.getBlock())
                .action(WorldLogAction.BLOCK_BREAK)
                .playerMetadata(event.getPlayer()).build();

        worldLogHandler.addLog(worldLog);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        WorldLog worldLog = WorldLog.builder()
                .block(event.getBlock())
                .action(WorldLogAction.BLOCK_PLACE)
                .playerMetadata(event.getPlayer()).build();

        worldLogHandler.addLog(worldLog);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
        Block block = event.getBlock();

        WorldLog worldLog = WorldLog.builder()
                .block(block)
                .action(WorldLogAction.BLOCK_DISPENSE)
                .itemMetadata(event.getItem())
                .build();

        worldLogHandler.addLog(worldLog);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            WorldLog worldLog = WorldLog.builder()
                    .block(block)
                    .metadata("Source", "Explosion")
                    .action(WorldLogAction.BLOCK_EXPLODE)
                    .build();

            worldLogHandler.addLog(worldLog);
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveEvent(InventoryMoveItemEvent event) {

        Inventory destination = event.getDestination();
        if (destination.getLocation() == null) return;

        Inventory source = event.getSource();
        if (source.getLocation() == null) return;

        WorldLog log = WorldLog.builder()
                .block(destination.getLocation().getBlock())
                .action(WorldLogAction.BLOCK_MOVE_ITEM)
                .metadata("Source", source.getLocation().getBlock().getType().name())
                .itemMetadata(event.getItem())
                .build();

        worldLogHandler.addLog(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickupEvent(InventoryPickupItemEvent event) {

        Inventory inventory = event.getInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (inventory.getLocation() == null) return;

        Block block = inventory.getLocation().getBlock();
        if (inventoryHolder instanceof DoubleChest doubleChest) {
            block = doubleChest.getLocation().getBlock();
        }

        WorldLog log = WorldLog.builder()
                .block(block)
                .action(WorldLogAction.BLOCK_PICKUP_ITEM)
                .itemMetadata(event.getItem().getItemStack())
                .build();

        worldLogHandler.addLog(log);
    }

    // Always shows the source block as air, despite documentation suggesting otherwise
    //@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    //public void onBlockDropItemEvent(BlockDropItemEvent event) {
    //    Block block = event.getBlockState().getBlock();
    //    if(block.getState() instanceof Container) {
    //        event.getItems().forEach(item -> {
    //            WorldLog log = WorldLog.builder()
    //                    .block(block)
    //                    .action(WorldLogAction.BLOCK_DROP_ITEM)
    //                    .itemMetadata(item.getItemStack())
    //                    .build();
    //            worldLogHandler.addLog(log);
    //        });
    //    }
    //}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        ItemStack itemStack = event.getEntity().getItemStack();
        WorldLog log = WorldLog.builder()
                .location(event.getLocation())
                .material(itemStack.getType())
                .action(WorldLogAction.ITEM_DEPSAWN)
                .itemMetadata(itemStack)
                .build();

        worldLogHandler.addLog(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {

        WorldLog log = WorldLog.builder()
                .location(event.getPlayer().getLocation())
                .material(event.getItemDrop().getItemStack().getType())
                .action(WorldLogAction.ENTITY_DROP_ITEM)
                .itemMetadata(event.getItemDrop().getItemStack())
                .entityMetadata(event.getPlayer())
                .build();

        worldLogHandler.addLog(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {

        WorldLog log = WorldLog.builder()
                .location(event.getEntity().getLocation())
                .material(event.getItem().getItemStack().getType())
                .action(WorldLogAction.ENTITY_PICKUP_ITEM)
                .itemMetadata(event.getItem().getItemStack())
                .entityMetadata(event.getEntity())
                .build();

        worldLogHandler.addLog(log);
    }

    /**
     * Tracks when an item has the picked up action. Needed, as if a player exits the inventory while holding an item in the cursor, not further event is fired
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickup(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("PICKUP")) {
                lastInventory.put(player, event.getClickedInventory());
                lastHeldItem.put(player, event.getCurrentItem());

            }
        }
    }

    /**
     * Tracks when an item is placed in an inventory.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("PLACE") && !event.getSlotType().equals(InventoryType.SlotType.FUEL) && event.getClickedInventory() != null) {
                placeItemLogic(player, Objects.requireNonNull(event.getClickedInventory()), event.getCursor());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventorySwapWithCursor(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
                if (!event.getSlotType().equals(InventoryType.SlotType.FUEL)) {
                    //cannot place an UUIDItem in a fuel slot
                    placeItemLogic(player, Objects.requireNonNull(event.getClickedInventory()), event.getCursor());
                }

                //now do pickup logic

                lastInventory.put(player, event.getClickedInventory());
                lastHeldItem.put(player, event.getCurrentItem());

            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryHotbar(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("HOTBAR")) {
                if (!Objects.requireNonNull(event.getClickedInventory()).getType().equals(InventoryType.PLAYER)) {
                    processRetrieveItem(player, event.getClickedInventory(), event.getCurrentItem());
                    UtilServer.runTaskLater(core, false, () -> processStoreItemInSlot(player, event.getClickedInventory(), event.getSlot()), 1);
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
                if (inventory == null) return;

                if (INVENTORY_FURNACE_TYPES.contains(event.getInventory().getType()) && !event.getInventory().equals(event.getClickedInventory())) {
                    //this is a furnace, UUIDItems cannot be shift clicked in, but can be shift clicked out
                    return;
                }
                if (inventory.getType().equals(InventoryType.PLAYER)) {
                    processStoreItem(player, event.getInventory(), event.getCurrentItem());
                } else {
                    processRetrieveItem(player, inventory, event.getCurrentItem());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCloseInventory(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (lastHeldItem.containsKey(player) && lastInventory.containsKey(player)) {
                //the player is holding an item, and the inventory has closed. This means they have the item.
                ItemStack item = lastHeldItem.get(player);
                Inventory inventory = lastInventory.get(player);
                if (inventory.getType().equals(InventoryType.PLAYER)) {
                    //The last inventory is player, so not actually retrieving
                    lastHeldItem.remove(player);
                    lastInventory.remove(player);
                    return;
                }
                Location location = inventory.getLocation();
                if(inventory.getHolder() instanceof DoubleChest doubleChest) {
                    location = doubleChest.getLocation();
                }
                if (location == null) return;

                WorldLog log = WorldLog.builder()
                        .location(location)
                        .block(location.getBlock())
                        .action(WorldLogAction.CONTAINER_WITHDRAW_ITEM)
                        .itemMetadata(item)
                        .entityMetadata(player)
                        .build();

                worldLogHandler.addLog(log);

                lastHeldItem.remove(player);
                lastInventory.remove(player);
            }
        }
    }

    private void placeItemLogic(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //This is an inventory that can store items
            if (lastInventory.containsKey(player)) {
                if (lastInventory.get(player) != inventory) {
                    if (inventory.getType().equals(InventoryType.PLAYER)) {
                        processRetrieveItem(player, lastInventory.get(player), itemStack);
                    } else {
                        processStoreItem(player, inventory, itemStack);
                    }
                }
            }
            lastInventory.remove(player);
            lastHeldItem.remove(player);
        }

    }

    private void processRetrieveItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items, therefore we can retrieve from it
            Location location = inventory.getLocation();
            if(inventory.getHolder() instanceof DoubleChest doubleChest) {
                location = doubleChest.getLocation();
            }
            if (location == null) return;

            WorldLog log = WorldLog.builder()
                    .location(location)
                    .block(location.getBlock())
                    .action(WorldLogAction.CONTAINER_WITHDRAW_ITEM)
                    .itemMetadata(itemStack)
                    .entityMetadata(player)
                    .build();

            worldLogHandler.addLog(log);

        }
    }

    private void processStoreItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items
            Location location = inventory.getLocation();
            if(inventory.getHolder() instanceof DoubleChest doubleChest) {
                location = doubleChest.getLocation();
            }
            if (location == null) return;

            WorldLog log = WorldLog.builder()
                    .location(location)
                    .block(location.getBlock())
                    .action(WorldLogAction.CONTAINER_DEPOSIT_ITEM)
                    .itemMetadata(itemStack)
                    .entityMetadata(player)
                    .build();

            worldLogHandler.addLog(log);

        }
    }

    private void processStoreItemInSlot(Player player, Inventory inventory, int slot) {
        processStoreItem(player, inventory, inventory.getItem(slot));
    }


    @UpdateEvent(delay = 1000)
    public void saveWorldLogs() {
        if (worldLogHandler.getPendingLogs().isEmpty()) return;

        synchronized (worldLogHandler.getPendingLogs()) {
            List<WorldLog> logs = new ArrayList<>(worldLogHandler.getPendingLogs());
            worldLogHandler.getPendingLogs().clear();

            worldLogHandler.saveLogs(logs);
        }
    }
}
