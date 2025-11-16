package me.mykindos.betterpvp.core.item.component.impl.uuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemLootEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.PendingLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@BPvPListener
@CustomLog
public class UUIDController implements Listener {


    private final Core core;

    private final ItemFactory itemFactory;

    private final ClientManager clientManager;

    private final Set<UUID> uuidSet = new HashSet<>();

    private static final double UUID_CHECK_TIME_SECONDS = 120;

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
    private final Map<Player, UUIDEntry> lastHeldUUIDItem = new WeakHashMap<>();
    private final UUIDManager uuidManager;

    @Inject
    public UUIDController(Core core, ItemFactory itemFactory, ClientManager clientManager, UUIDManager uuidManager) {
        this.core = core;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        this.uuidManager = uuidManager;
    }

    public Optional<UUIDItem> getUUIDItem(ItemStack itemStack) {
        if (itemStack != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
                if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                    return uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING))).toString());
                }
            }
        }
        return Optional.empty();
    }

    public Map<UUIDItem, ItemInstance> getUUIDItems(Player player) {
        Map<UUIDItem, ItemInstance> uuidItemList = new HashMap<>();
        player.getInventory().forEach(itemStack -> {
            if (itemStack != null) {
                Optional<UUIDItem> uuidItemOptional = getUUIDItem(itemStack);
                uuidItemOptional.ifPresent(item -> {
                    uuidItemList.put(item, itemFactory.fromItemStack(itemStack).orElseThrow());
                });
            }
        });
        return uuidItemList;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDDrop(SpecialItemLootEvent event) {
        getUUIDItem(event.getItemInstance().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getLocation();
            log.info("({}) was looted from {} at {}",
                    uuidItem.getUuid(), event.getSource(), UtilWorld.locationToString(location))
                    .setAction("ITEM_LOOT_SPAWN")
                    .addItemContext(itemFactory.getItemRegistry(), event.getItemInstance())
                    .addContext(LogContext.SOURCE, event.getSource())
                    .addLocationContext(location).submit();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItem().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getEntity().getLocation();
        PendingLog pendingLog = log.info("{} picked up ({}) at {}",
                event.getEntity().getName(), uuidItem.getUuid(), UtilWorld.locationToString(location))
                .setAction("ITEM_PICKUP").addLocationContext(location);

        if (event.getEntity() instanceof Player player) {
            pendingLog.addClientContext(player);
        }

        final ItemInstance instance = itemFactory.fromItemStack(event.getItem().getItemStack()).orElseThrow();
        pendingLog.addItemContext(itemFactory.getItemRegistry(), instance).submit();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDDrop(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;

        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItemDrop().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getPlayer().getLocation();
        log.info("{} dropped ({}) at ({})", event.getPlayer().getName(), uuidItem.getUuid(), UtilWorld.locationToString(location))
                .setAction("ITEM_DROP")
                .addClientContext(event.getPlayer())
                .addLocationContext(location)
                .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(event.getItemDrop().getItemStack()).orElseThrow())
                .submit();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserKilled(KillContributionEvent event) {
        final Player victim = event.getVictim();
        Map<UUIDItem, ItemInstance> uuidItemsList = getUUIDItems(victim);
        if (uuidItemsList.isEmpty()) return;
        final Player killer = event.getKiller();
        final Map<Player, Contribution> contributions = event.getContributions();


        StringBuilder contributors = new StringBuilder();
        contributors.append("<yellow>");
        for (Player player : contributions.keySet()) {
            contributors.append(player.getName()).append(", ");
        }
        Location location = victim.getLocation();
        for (UUIDItem item : uuidItemsList.keySet()) {
            log.info("{} was killed while holding ({}) by {} at ({}), contributed by {}",
                            victim.getName(), item.getUuid(), killer.getName(), UtilWorld.locationToString(location), contributors)
                    .setAction("ITEM_DEATH_PLAYER")
                    .addLocationContext(location)
                    .addContext("Victim", victim.getName())
                    .addContext("Killer", killer.getName())
                    .addContext("Contributors", contributors.toString())
                    .addItemContext(itemFactory.getItemRegistry(), uuidItemsList.get(item))
                    .submit();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Map<UUIDItem, ItemInstance> uuidItemsList = getUUIDItems(player);
        if (uuidItemsList.isEmpty()) return;
        Location location = player.getLocation();
        for (UUIDItem item : uuidItemsList.keySet()) {
            log.info("{} died with ({}) at ({})", player.getName(), item.getUuid(), UtilWorld.locationToString(location))
                    .setAction("ITEM_DEATH")
                    .addClientContext(player)
                    .addItemContext(itemFactory.getItemRegistry(), uuidItemsList.get(item))
                    .addLocationContext(location)
                    .submit();
        }
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
                Optional<UUIDItem> uuidItemsOptional = getUUIDItem(event.getCurrentItem());
                if (uuidItemsOptional.isPresent()) {
                    lastInventory.put(player, event.getClickedInventory());
                    lastHeldUUIDItem.put(player, new UUIDEntry(
                            uuidItemsOptional.get(),
                            itemFactory.fromItemStack(Objects.requireNonNull(event.getCurrentItem())).orElseThrow()
                    ));
                }
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
                Optional<UUIDItem> uuidItemsOptional = getUUIDItem(event.getCurrentItem());
                if (uuidItemsOptional.isPresent()) {
                    lastInventory.put(player, event.getClickedInventory());
                    lastHeldUUIDItem.put(player, new UUIDEntry(
                            uuidItemsOptional.get(),
                            itemFactory.fromItemStack(Objects.requireNonNull(event.getCurrentItem())).orElseThrow()
                    ));
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
            if (lastHeldUUIDItem.containsKey(player) && lastInventory.containsKey(player)) {
                //the player is holding an item, and the inventory has closed. This means they have the item.
                UUIDEntry entry = lastHeldUUIDItem.get(player);
                UUIDItem item = entry.uuidItem();
                ItemInstance instance = entry.instance();

                Inventory inventory = lastInventory.get(player);
                if (inventory.getType().equals(InventoryType.PLAYER)) {
                    //The last inventory is player, so not actually retrieving
                    lastHeldUUIDItem.remove(player);
                    lastInventory.remove(player);
                    return;
                }
                Location location = inventory.getLocation();
                assert location != null;
                log.info("{} retrieved ({}) from {} at {}", player.getName(), item.getUuid(),
                                Objects.requireNonNull(inventory).getType().name(), UtilWorld.locationToString(location))
                        .setAction("ITEM_RETRIEVE")
                        .addClientContext(player)
                        .addLocationContext(location)
                        .addItemContext(itemFactory.getItemRegistry(), instance)
                        .addBlockContext(location.getBlock())
                        .submit();

                lastHeldUUIDItem.remove(player);
                lastInventory.remove(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(ClientJoinEvent event) {
        Location location = event.getPlayer().getLocation();
        getUUIDItems(event.getPlayer()).forEach((uuidItem, item) -> {
            log.info("{} Logged in with ({}) at {}", event.getPlayer().getName(), uuidItem.getUuid(), UtilWorld.locationToString(location))
                    .setAction("ITEM_LOGIN")
                    .addClientContext(event.getPlayer())
                    .addLocationContext(location)
                    .addItemContext(itemFactory.getItemRegistry(), item)
                    .submit();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogout(ClientQuitEvent event) {
        processExit(event.getPlayer());
    }

    /**
     * Prevent UUID items from being duplicated in creative
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemClone(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getAction().equals(InventoryAction.CLONE_STACK)) {
                getUUIDItem(event.getCurrentItem()).ifPresent(uuidItem -> {
                    event.setCancelled(true);
                    UtilMessage.message(player, "Core", "You cannot clone this item.");
                });
            }
        }
    }

    /**
     * Prevent UUID items from being fuel
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPutInFuel(InventoryClickEvent event) {
        if (event.getSlotType().equals(InventoryType.SlotType.FUEL)) {
            if (getUUIDItem(event.getCursor()).isPresent()) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    UtilMessage.message(player, "Core", UtilMessage.deserialize("You cannot use this item as fuel."));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMusicDiskPlay(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (Objects.requireNonNull(event.getClickedBlock()).getType().equals(Material.JUKEBOX)) {
                if (getUUIDItem(event.getItem()).isPresent()) {
                    //prevent using an UUIDItem in a Jukebox
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        getUUIDItem(event.getEntity().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getEntity().getLocation();
            log.info("{} despawned at ({})", uuidItem.getUuid(), UtilWorld.locationToString(location))
                    .setAction("ITEM_DESPAWN")
                    .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(event.getEntity().getItemStack()).orElseThrow())
                    .addLocationContext(location)
                    .submit();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMoveEvent(InventoryMoveItemEvent event) {
        getUUIDItem(event.getItem()).ifPresent(uuidItem -> {
            Location locationSource = event.getSource().getLocation();
            Location locationDestination = event.getDestination().getLocation();
            assert locationSource != null;
            assert locationDestination != null;
            log.info("({}) moved from {} {} to {} {}", uuidItem.getUuid(),
                            event.getSource().getType().toString(), UtilWorld.locationToString(locationSource),
                            event.getDestination().getType().toString(), UtilWorld.locationToString(locationDestination))
                    .setAction("ITEM_INVENTORY_MOVE")
                    .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(event.getItem()).orElseThrow())
                    .addContext(LogContext.CURRENT_LOCATION, UtilWorld.locationToString(locationSource))
                    .addContext(LogContext.NEW_LOCATION, UtilWorld.locationToString(locationDestination)).submit();

        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickupEvent(InventoryPickupItemEvent event) {
        getUUIDItem(event.getItem().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getInventory().getLocation();
            assert location != null;
            log.info("({}) was picked up by block {} at {}", uuidItem.getUuid(), event.getInventory().getType().name(), UtilWorld.locationToString(location))
                    .setAction("ITEM_INVENTORY_PICKUP")
                    .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(event.getItem().getItemStack()).orElseThrow())
                    .addLocationContext(location)
                    .addBlockContext(location.getBlock())
                    .submit();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDropItemEvent(BlockDropItemEvent event) {
        if (event.isCancelled()) return;
        event.getItems().forEach(item -> {
            getUUIDItem(item.getItemStack()).ifPresent(uuidItem -> {
                Location location = event.getBlock().getLocation();
                log.info("{} caused ({}) to be dropped from block {} at {}", event.getPlayer().getName(),
                                uuidItem.getUuid(), event.getBlockState().getType().name(), UtilWorld.locationToString(location))
                        .setAction("ITEM_CONTAINER_BREAK")
                        .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(item.getItemStack()).orElseThrow())
                        .addLocationContext(location)
                        .addBlockContext(event.getBlockState())
                        .addClientContext(event.getPlayer()).submit();
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
        getUUIDItem(event.getItem()).ifPresent(uuidItem -> {
            Location location = event.getBlock().getLocation();
            log.info("({}) was dispensed from block {} at {}", uuidItem.getUuid(), event.getBlock().getType().name(),
                            UtilWorld.locationToString(location)).setAction("ITEM_BLOCK_DISPENSE")
                    .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(event.getItem()).orElseThrow())
                    .addLocationContext(location)
                    .addBlockContext(event.getBlock())
                    .submit();

        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (block.getState() instanceof Container container) {
                container.getInventory().forEach(itemStack -> {
                    getUUIDItem(itemStack).ifPresent(uuidItem -> {
                        Location location = container.getLocation();
                        log.info("({}) was dropped due to explosion from {} at {}", uuidItem.getUuid(), container.getType().name(),
                                        UtilWorld.locationToString(location)).setAction("ITEM_CONTAINER_EXPLODE")
                                .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(itemStack).orElseThrow())
                                .addLocationContext(location)
                                .addBlockContext(block)
                                .submit();
                    });
                });
            }
        });
    }

    @UpdateEvent(delay = (long) (1 * 1000 * UUID_CHECK_TIME_SECONDS))
    public void checkPlayers() {
        log.info("Checking Players for duplicate items").submit();
        uuidSet.clear();
        AtomicInteger duplicates = new AtomicInteger();
        for (Player player : Bukkit.getOnlinePlayers()) {
            getUUIDItems(player).forEach((uuidItem, item) -> {
                if (!uuidSet.add(uuidItem.getUuid())) {
                    Component component = UtilMessage.deserialize("<red>WARNING</red> Potential duplicate UUID found in ")
                            .append(UtilMessage.deserialize("<yellow>%s</yellow>", player.getName())
                                    .clickEvent(ClickEvent.runCommand("/search player " + player.getName()))
                                    .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to search by Player"))))
                            .appendSpace()
                            .append(UtilMessage.deserialize("<light_purple>%s</light_purple>", uuidItem.getUuid().toString())
                                    .clickEvent(ClickEvent.runCommand("/search item " + uuidItem.getUuid().toString()))
                                    .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to search by UUID"))))
                            .append(UtilMessage.deserialize(" (<green>%s</green>)", uuidItem.getIdentifier()));
                    clientManager.sendMessageToRank("Core", component, Rank.TRIAL_MOD);
                    log.info("Potential duplicate ({}) found in player {}", uuidItem.getUuid(), player.getUniqueId()).submit();
                    duplicates.getAndIncrement();
                }
            });
        }
        log.info("Checking {} players for UUIDItem duplicates, found {}", Bukkit.getOnlinePlayers().size(), duplicates.get()).submit();
    }

    private void processExit(Player player) {
        if (lastHeldUUIDItem.containsKey(player) && lastInventory.containsKey(player)) {
            //the player is holding an item, and the inventory has closed. This means they have the item.
            UUIDEntry entry = lastHeldUUIDItem.get(player);
            UUIDItem item = entry.uuidItem();
            ItemInstance instance = entry.instance();

            Inventory inventory = lastInventory.get(player);
            if (inventory.getType().equals(InventoryType.PLAYER)) {
                //The last inventory is player, so not actually retrieving
                lastHeldUUIDItem.remove(player);
                lastInventory.remove(player);
                return;
            }
            Location location = inventory.getLocation();
            assert location != null;
            log.info("({}) dropped due to disconnect ({}) from {} at ({})", player.getName(), item.getUuid(),
                            Objects.requireNonNull(inventory).getType().name(), UtilWorld.locationToString(location))
                    .setAction("ITEM_DROP")
                    .addClientContext(player)
                    .addItemContext(itemFactory.getItemRegistry(), instance)
                    .addLocationContext(location)
                    .submit();


            lastHeldUUIDItem.remove(player);
            lastInventory.remove(player);
        }
        Location location = player.getLocation();
        getUUIDItems(player).forEach((uuidItem, item) -> {
            log.info("{} Logged out with ({}) at ({})", player.getName(), uuidItem.getUuid(), UtilWorld.locationToString(location))
                    .setAction("ITEM_LOGOUT")
                    .addClientContext(player)
                    .addItemContext(itemFactory.getItemRegistry(), item)
                    .addLocationContext(location)
                    .submit();

        });
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
            lastHeldUUIDItem.remove(player);
        }

    }

    private void processRetrieveItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items, therefore we can retrieve from it
            getUUIDItem(itemStack).ifPresent(item -> {
                Location location = inventory.getLocation();
                assert location != null;
                log.info("{} retrieved ({}) from {} at ({})", player.getName(), item.getUuid(),
                                Objects.requireNonNull(inventory).getType().name(), UtilWorld.locationToString(location))
                        .setAction("ITEM_RETRIEVE")
                        .addClientContext(player)
                        .addLocationContext(location)
                        .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(itemStack).orElseThrow())
                        .addBlockContext(location.getBlock()).submit();

            });

        }
    }

    private void processStoreItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items
            getUUIDItem(itemStack).ifPresent(item -> {
                Location location = inventory.getLocation();
                assert location != null;
                log.info("{} stored ({}) in {} at ({})", player.getName(), item.getUuid(), inventory.getType().name(), UtilWorld.locationToString(location))
                        .setAction("ITEM_CONTAINER_STORE")
                        .addClientContext(player)
                        .addItemContext(itemFactory.getItemRegistry(), itemFactory.fromItemStack(itemStack).orElseThrow())
                        .addBlockContext(location.getBlock())
                        .addLocationContext(location).submit();

            });
        }
    }

    private void processStoreItemInSlot(Player player, Inventory inventory, int slot) {
        processStoreItem(player, inventory, inventory.getItem(slot));
    }

    private record UUIDEntry(UUIDItem uuidItem, ItemInstance instance) {

    }

}