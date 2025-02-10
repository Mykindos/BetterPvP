package me.mykindos.betterpvp.core.world.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private final WorldLogHandler worldLogHandler;

    @Inject
    public WorldLogListener(WorldLogHandler worldLogHandler) {
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

        worldLogHandler.getWorldLogRepository().processSession(player, session, 1);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInspectBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!worldLogHandler.getInspectingPlayers().contains(player.getUniqueId())) return;

        event.setCancelled(true);

        // Lookups
        WorldLogSession session = worldLogHandler.getSession(player.getUniqueId());
        session.setStatement(worldLogHandler.getWorldLogRepository().getStatementForBlock(event.getBlock()));

        worldLogHandler.getWorldLogRepository().processSession(player, session, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakBlock(BlockBreakEvent event) {
        WorldLog worldLog = WorldLog.builder()
                .block(event.getBlock())
                .action(WorldLogAction.BLOCK_BREAK)
                .playerMetadata(event.getPlayer()).build();

        worldLogHandler.getPendingLogs().add(worldLog);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        WorldLog worldLog = WorldLog.builder()
                .block(event.getBlock())
                .action(WorldLogAction.BLOCK_PLACE)
                .playerMetadata(event.getPlayer()).build();

        worldLogHandler.getPendingLogs().add(worldLog);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
        Block block = event.getBlock();

        WorldLog worldLog = WorldLog.builder()
                .block(block)
                .action(WorldLogAction.BLOCK_DISPENSE)
                .itemMetadata(event.getItem())
                .build();

        worldLogHandler.getPendingLogs().add(worldLog);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        List<WorldLog> logs = new ArrayList<>();
        for (Block block : event.blockList()) {
            WorldLog worldLog = WorldLog.builder()
                    .block(block)
                    .action(WorldLogAction.BLOCK_EXPLODE)
                    .build();

            logs.add(worldLog);
        }

        worldLogHandler.getPendingLogs().addAll(logs);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveEvent(InventoryMoveItemEvent event) {

        Inventory destination = event.getDestination();
        if(destination.getLocation() == null) return;

        WorldLog log = WorldLog.builder()
                .block(destination.getLocation().getBlock())
                .action(WorldLogAction.BLOCK_MOVE_ITEM)
                .itemMetadata(event.getItem())
                .build();

        worldLogHandler.getPendingLogs().add(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickupEvent(InventoryPickupItemEvent event) {

        Inventory inventory = event.getInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();

        if(inventory.getLocation() == null) return;

        Block block = inventory.getLocation().getBlock();
        if(inventoryHolder instanceof DoubleChest doubleChest) {
            block = doubleChest.getLocation().getBlock();
        }

       WorldLog log = WorldLog.builder()
               .block(block)
               .action(WorldLogAction.BLOCK_PICKUP_ITEM)
               .itemMetadata(event.getItem().getItemStack())
               .build();

        worldLogHandler.getPendingLogs().add(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDropItemEvent(BlockDropItemEvent event) {
        List<WorldLog> logs = new ArrayList<>();
        event.getItems().forEach(item -> {
           WorldLog log = WorldLog.builder()
                   .block(event.getBlock())
                   .action(WorldLogAction.BLOCK_DROP_ITEM)
                   .itemMetadata(item.getItemStack())
                   .build();

           logs.add(log);
        });

        worldLogHandler.getPendingLogs().addAll(logs);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        ItemStack itemStack = event.getEntity().getItemStack();
        WorldLog log = WorldLog.builder()
                .location(event.getLocation())
                .material(itemStack.getType())
                .action(WorldLogAction.ITEM_DEPSAWN)
                .itemMetadata(itemStack)
                .build();

        worldLogHandler.getPendingLogs().add(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        System.out.println("A");
        WorldLog log = WorldLog.builder()
                .location(event.getPlayer().getLocation())
                .material(event.getItemDrop().getItemStack().getType())
                .action(WorldLogAction.ENTITY_DROP_ITEM)
                .itemMetadata(event.getItemDrop().getItemStack())
                .entityMetadata(event.getPlayer())
                .build();

        worldLogHandler.getPendingLogs().add(log);
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {

        WorldLog log = WorldLog.builder()
                .location(event.getEntity().getLocation())
                .material(event.getItem().getItemStack().getType())
                .action(WorldLogAction.ENTITY_PICKUP_ITEM)
                .itemMetadata(event.getItem().getItemStack())
                .entityMetadata(event.getEntity())
                .build();

        worldLogHandler.getPendingLogs().add(log);
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
