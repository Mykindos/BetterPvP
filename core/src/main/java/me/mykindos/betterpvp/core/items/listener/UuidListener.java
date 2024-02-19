package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.logger.UUIDItem;
import me.mykindos.betterpvp.core.items.logger.UUIDManager;
import me.mykindos.betterpvp.core.items.logger.UuidLogger;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItem().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getEntity().getLocation();
        int logID = UuidLogger.logID("%s picked up %s at (%s, %s, %s) in %s", event.getEntity().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        if (event.getEntity() instanceof Player player) {
            UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, player.getUniqueId());
        } else {
            UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, null);
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
        int logID = UuidLogger.logID("%s dropped %s at (%s, %s, %s) in %s", event.getPlayer().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.DROP, event.getPlayer().getUniqueId());
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
            int logID = UuidLogger.logID("%s was killed while holding %s by %s at (%s, %s, %s) in %s, contributed by %s", victim.getName(), item.getUuid(), killer.getName(), victim.getLocation().toString(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName(), contributors);
            if (logID < 0) {
                continue;
            }
            UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.DEATH, victim.getUniqueId());
            UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.KILL, killer.getUniqueId());
            for (Player player : contributions.keySet()) {
                UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.CONTRIBUTOR, player.getUniqueId());
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
            int logID = UuidLogger.logID("%s died while holding %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            if (logID < 0) {
                continue;
            }
            UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.DEATH, player.getUniqueId());
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
                    UUIDItem uuidItem = uuidItemsOptional.get();
                    Location location = player.getLocation();
                    lastInventory.put(player, event.getClickedInventory());
                    /*
                    int logID = UuidLogger.logID("%s retrieved %s from %s at (%s, %s, %s) in %s", player.getName(), uuidItem.getUuid(), Objects.requireNonNull(event.getClickedInventory()).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                    if (logID < 0) {
                        return;
                    }
                    UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.RETREIVE, player.getUniqueId());
                     */
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
                Optional<UUIDItem> UuidItemOptional = getUUIDItem(event.getCursor());
                if (UuidItemOptional.isPresent()) {
                    //we are placing a UUID item
                    UUIDItem item = UuidItemOptional.get();
                    Location location = player.getLocation();
                    assert lastInventory.containsKey(player);
                    if (lastInventory.get(player) == event.getClickedInventory()) {
                        //this is a move between the same inventory. We don't care about it.
                        log.info("Last inventory the same, don't need to update");
                        lastInventory.remove(player);
                        return;
                    }
                    int logID = UuidLogger.logID("%s placed %s in %s from %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), Objects.requireNonNull(event.getClickedInventory()).getType().name(), lastInventory.get(player).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                    if (logID < 0) {
                        lastInventory.remove(player);
                        return;
                    }
                    UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.RETREIVE, player.getUniqueId());
                    lastInventory.remove(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventorySwapWithCursor(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)){
                //first, do placing logic
                Optional<UUIDItem> UuidItemOptional1 = getUUIDItem(event.getCursor());
                if (UuidItemOptional1.isPresent()) {
                    //we are placing a UUID item
                    UUIDItem item = UuidItemOptional1.get();
                    Location location = player.getLocation();
                    assert lastInventory.containsKey(player);
                    if (lastInventory.get(player) == event.getClickedInventory()) {
                        log.info("Last inventory the same, don't need to update");
                    } else {
                        int logID = UuidLogger.logID("%s placed %s in %s from %s at (%s, %s, %s) in %s", player.getName(), item.getUuid(), Objects.requireNonNull(event.getClickedInventory()).getType().name(), lastInventory.get(player).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                        if (logID >= 0) {
                            UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.RETREIVE, player.getUniqueId());
                        }
                    }
                    lastInventory.remove(player);
                }
                //now do pickup logic
                Optional<UUIDItem> uuidItemsOptional = getUUIDItem(event.getCurrentItem());
                if (uuidItemsOptional.isPresent()) {
                    lastInventory.put(player, event.getClickedInventory());
                }
            }
        }
    }


    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event) {
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

    private List<UUIDItem> getUUIDItems(Player player) {
        List<UUIDItem> uuidItemList = new ArrayList<>();
        player.getInventory().forEach(itemStack -> {
            if (itemStack != null) {
                Optional<UUIDItem> uuidItemOptional = getUUIDItem(itemStack);
                if (uuidItemOptional.isPresent()) {
                    uuidItemList.add(getUUIDItem(itemStack).orElseThrow());
                }
            }
        });
        return uuidItemList;
    }

    private Optional<UUIDItem> getUUIDItem(ItemStack itemStack) {
        if (itemStack != null) {
            PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
            if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                return uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING))));
            }
        }
        return Optional.empty();
    }
}


