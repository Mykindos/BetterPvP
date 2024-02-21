package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.logger.UUIDItem;
import me.mykindos.betterpvp.core.items.logger.UUIDManager;
import me.mykindos.betterpvp.core.items.logger.UuidLogger;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
@Slf4j
public class UuidListener implements Listener {


    @Inject
    Core core;

    @Inject
    UUIDManager uuidManager;

    private final Map<Player, Inventory> lastInventory = new HashMap<>();
    private final Map<Player, UUIDItem> lastHeldUUIDItem = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItem().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getEntity().getLocation();
        UUID logID = UuidLogger.legend("%s picked up %s at (%s, %s, %s) in %s", event.getEntity().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        if (event.getEntity() instanceof Player player) {
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, player.getUniqueId());
        } else {
            UuidLogger.AddItemUUIDMetaInfoNone(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDDrop(PlayerDropItemEvent event) {
        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItemDrop().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getPlayer().getLocation();
        UUID logID = UuidLogger.legend("%s dropped %s at (%s, %s, %s) in %s", event.getPlayer().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        UuidLogger.AddItemUUIDMetaInfoPlayer(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.DROP, event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserKilled(KillContributionEvent event) {
        final Player victim = event.getVictim();
        List<UUIDItem> uuidItemsList = getUUIDItems(victim);
        if (uuidItemsList.isEmpty()) return;
        final Player killer = event.getKiller();
        final Map<Player, Contribution> contributions = event.getContributions();


        StringBuilder contributors = new StringBuilder();
        contributors.append("");
        for (Player player : contributions.keySet()) {
            contributors.append(player.getName()).append(", ");
        }
        Location location = victim.getLocation();
        for (UUIDItem item : uuidItemsList) {
            UUID logID = UuidLogger.legend("%s was killed while holding %s by %s at (%s, %s, %s) in %s, contributed by %s", victim.getName(), item.getUuid(), killer.getName(), victim.getLocation().toString(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName(), contributors);
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.DEATH, victim.getUniqueId());
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.KILL, killer.getUniqueId());
            for (Player player : contributions.keySet()) {
                UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.CONTRIBUTOR, player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        List<UUIDItem> uuidItemsList = getUUIDItems(player);
        if (uuidItemsList.isEmpty()) return;
        Location location = player.getLocation();
        for (UUIDItem item : uuidItemsList) {
            UUID logID = UuidLogger.legend("%s died while holding %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.DEATH, player.getUniqueId());
        }
    }

    /**
     * Tracks when an item has the picked up action. Needed, as if a player exits the inventory while holding an item in the cursor, not further event is fired
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickup(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("PICKUP")) {
                Optional<UUIDItem> uuidItemsOptional = getUUIDItem(event.getCurrentItem());
                if (uuidItemsOptional.isPresent()) {
                    lastInventory.put(player, event.getClickedInventory());
                    lastHeldUUIDItem.put(player, uuidItemsOptional.get());
                }
            }
        }
    }

    /**
     * Tracks when an item is placed in an inventory.
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().name().contains("PLACE")){
                placeItemLogic(player, event.getClickedInventory(), event.getCursor());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventorySwapWithCursor(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
                placeItemLogic(player, event.getClickedInventory(), event.getCursor());
                //now do pickup logic
                Optional<UUIDItem> uuidItemsOptional = getUUIDItem(event.getCurrentItem());
                if (uuidItemsOptional.isPresent()) {
                    lastInventory.put(player, event.getClickedInventory());
                    lastHeldUUIDItem.put(player, uuidItemsOptional.get());
                }
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
                assert inventory != null;
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
            processExit(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogout(ClientQuitEvent event) {
        processExit(event.getPlayer());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processExit(player);
        }
    }

    /**
     * Prevent UUID items from being duplicated in creative
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemClone(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.CLONE_STACK)) {
                Optional<UUIDItem> UuidItemOptional = getUUIDItem(event.getCurrentItem());
                if (UuidItemOptional.isPresent()) {
                    event.setCancelled(true);
                    UtilMessage.message(player, "Core", "You cannot clone this item.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        getUUIDItem(event.getEntity().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getEntity().getLocation();
            UUID logUUID = UuidLogger.legend("%s despawned at (%s, %s, %s) in %s", uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UuidLogger.AddItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UuidLogger.UuidLogType.DESPAWN);
        });
    }

    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event) {
        //debug
        log.info("");
        log.info("Click Event");
        log.info("Action " + event.getAction().toString());
        log.info("Current " + (event.getCurrentItem() == null ? null : event.getCurrentItem().toString()));
        log.info("Cursor " + (event.getCursor() == null ? null : event.getCursor().toString()));
        log.info("Slot " + event.getSlot());
        log.info("Raw slot " + event.getRawSlot());
        log.info("Click " + event.getClick());
        log.info("Hotbar button " + event.getHotbarButton());
        log.info("SlotType " + event.getSlotType());
    }

    private void processExit(Player player) {
        if (lastHeldUUIDItem.containsKey(player) && lastInventory.containsKey(player)) {
            //the player is holding an item, and the inventory has closed. This means they have the item.
            UUIDItem item = lastHeldUUIDItem.get(player);
            Inventory inventory = lastInventory.get(player);
            Location location = inventory.getLocation();
            assert location != null;
            UUID logID = UuidLogger.legend("%s retrieved %s from %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.RETREIVE, player.getUniqueId());

            lastHeldUUIDItem.remove(player);
            lastInventory.remove(player);
        }
    }

    private void placeItemLogic(Player player, Inventory inventory, ItemStack itemStack) {
        if (lastInventory.containsKey(player)) {
            if (!(lastInventory.get(player) == inventory)) {
                assert inventory != null;
                if (inventory.getType().equals(InventoryType.PLAYER)) {
                    processRetrieveItem(player, lastInventory.get(player), itemStack);
                } else {
                    processStoreItem(player, inventory, itemStack);
                }
            }
        }
        lastInventory.remove(player);
        lastHeldUUIDItem.remove(player);
    }

    private void processRetrieveItem(Player player, Inventory inventory, ItemStack itemStack) {
        Optional<UUIDItem> UuidItemOptional = getUUIDItem(itemStack);
        if (UuidItemOptional.isPresent()) {
            UUIDItem item = UuidItemOptional.get();
            Location location = inventory.getLocation();
            assert location != null;
            UUID logID = UuidLogger.legend("%s retrieved %s from %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.RETREIVE, player.getUniqueId());
        }
    }

    private void processStoreItem(Player player, Inventory inventory, ItemStack itemStack) {
        Optional<UUIDItem> UuidItemOptional = getUUIDItem(itemStack);
        if (UuidItemOptional.isPresent()) {
            UUIDItem item = UuidItemOptional.get();
            Location location = inventory.getLocation();
            assert location != null;
            UUID logID = UuidLogger.legend("%s stored %s in %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), inventory.getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UuidLogger.AddItemUUIDMetaInfoPlayer(logID, item.getUuid(), UuidLogger.UuidLogType.CONTAINER_STORE, player.getUniqueId());
        }
    }

    private void processStoreItemInSlot(Player player, Inventory inventory, int slot) {
        processStoreItem(player, inventory, inventory.getItem(slot));
    }

    private List<UUIDItem> getUUIDItems(Player player) {
        List<UUIDItem> uuidItemList = new ArrayList<>();
        player.getInventory().forEach(itemStack -> {
            if (itemStack != null) {
                Optional<UUIDItem> uuidItemOptional = getUUIDItem(itemStack);
                uuidItemOptional.ifPresent(uuidItemList::add);
            }
        });
        return uuidItemList;
    }

    private Optional<UUIDItem> getUUIDItem(ItemStack itemStack) {
        if (itemStack != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
                if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                    return uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING))));
                }
            }
        }
        return Optional.empty();
    }
}


