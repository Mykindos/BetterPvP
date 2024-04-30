package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.UUIDType;
import me.mykindos.betterpvp.core.logging.type.logs.ItemLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@BPvPListener
@CustomLog
public class UUIDListener implements Listener {


    private final Core core;

    private final ItemHandler itemHandler;

    private final ClientManager clientManager;

    private final UUIDManager uuidManager;

    private final Set<UUID> uuidSet = new HashSet<>();

    private static final double UUID_CHECK_TIME_SECONDS = 120;

    /**
     * A list of inventories that do not store items. I.e. a crafting table or anvil
     */
    private static final List<InventoryType> INVENTORY_NO_STORE_TYPES = new ArrayList<>(List.of(
            InventoryType.ANVIL,
            InventoryType.WORKBENCH
    ));

    private static final List<InventoryType> INVENTORY_FURNACE_TYPES = new ArrayList<>(List.of(
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER
    ));

    private final Map<Player, Inventory> lastInventory = new HashMap<>();
    private final Map<Player, UUIDItem> lastHeldUUIDItem = new HashMap<>();

    private final Map<UUID, Long> lastUUIDDropTime = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();

    @Inject
    public UUIDListener(Core core, ItemHandler itemHandler, ClientManager clientManager, UUIDManager uuidManager) {
        this.core = core;
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
        this.uuidManager = uuidManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUUIDItemDrop(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        Client client = clientManager.search().online(event.getPlayer());

        if (!(boolean) client.getProperty(ClientProperty.DROP_PROTECTION_ENABLED).orElse(false)) {
            return; // Drop protection is disabled
        }

        final Optional<UUIDItem> itemOpt = itemHandler.getUUIDItem(event.getItemDrop().getItemStack());
        if (itemOpt.isEmpty()) {
            return; // Not an UUIDItem
        }

        // Remove the drop attempt each time
        // It'll either be re-added if they couldn't drop the item, or the item will be dropped,
        // so we don't need to keep track of it
        final UUIDItem item = itemOpt.get();
        final long dropTime = Objects.requireNonNullElse(lastUUIDDropTime.remove(item.getUuid()), 0L);
        if (UtilTime.elapsed(dropTime, 500)) {
            // Didn't re-drop in time
            final long lastMessage = lastMessageTime.getOrDefault(client.getUniqueId(), 0L);
            if (UtilTime.elapsed(lastMessage, 5000)) {
                UtilMessage.message(event.getPlayer(), "Settings",
                        "<green>Drop Protection Enabled</green> <white>|</white> " +
                                "Double press <alt2><key:key.drop></alt2> to drop this item. " +
                                "Click <red><click:run_command:/settings>here</click></red> to turn this setting off.");
                lastMessageTime.put(client.getUniqueId(), System.currentTimeMillis());
            }

            new SoundEffect(Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 2F).play(event.getPlayer());
            event.setCancelled(true);

            // Log the drop attempt
            lastUUIDDropTime.put(item.getUuid(), System.currentTimeMillis());
            return;
        }

        lastMessageTime.remove(client.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        Optional<UUIDItem> uuidItemOptional = itemHandler.getUUIDItem(event.getItem().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getEntity().getLocation();
        UUID logID = log.info("{} picked up ({}) at ({}, {}, {}) in {}",
                event.getEntity().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        if (event.getEntity() instanceof Player player) {
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_PICKUP, uuidItem.getUuid())
                    .addLocation(location, null)
                    .addMeta(player.getUniqueId(), UUIDType.MAINPLAYER)
            );
        } else {
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_PICKUP, uuidItem.getUuid())
                    .addLocation(location, null)
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDDrop(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;

        Optional<UUIDItem> uuidItemOptional = itemHandler.getUUIDItem(event.getItemDrop().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        Location location = event.getPlayer().getLocation();
        UUID logID = log.info("{} dropped ({}) at ({}, {}, {}) in {}",
                event.getPlayer().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_DROP, uuidItem.getUuid())
                .addLocation(location, null)
                .addMeta(event.getPlayer().getUniqueId(), UUIDType.MAINPLAYER)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserKilled(KillContributionEvent event) {
        final Player victim = event.getVictim();
        List<UUIDItem> uuidItemsList = itemHandler.getUUIDItems(victim);
        if (uuidItemsList.isEmpty()) return;
        final Player killer = event.getKiller();
        final Map<Player, Contribution> contributions = event.getContributions();


        StringBuilder contributors = new StringBuilder();
        contributors.append("<yellow>");
        for (Player player : contributions.keySet()) {
            contributors.append(player.getName()).append(", ");
        }
        Location location = victim.getLocation();
        for (UUIDItem item : uuidItemsList) {
            UUID logID = log.info("{} was killed while holding ({}) by {} at ({}, {}, {}) in {}, contributed by {}",
                    victim.getName(), item.getUuid(), killer.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName(), contributors);
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_DEATH_PLAYER, item.getUuid())
                    .addLocation(location, null)
                    .addMeta(victim.getUniqueId(), UUIDType.MAINPLAYER)
                    .addMeta(killer.getUniqueId(), UUIDType.OTHERPLAYER)
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        List<UUIDItem> uuidItemsList = itemHandler.getUUIDItems(player);
        if (uuidItemsList.isEmpty()) return;
        Location location = player.getLocation();
        for (UUIDItem item : uuidItemsList) {
            UUID logID = log.info("{} died with ({}) at ({}, {}, {}) in {}",
                    player.getName(), item.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_DEATH, item.getUuid())
                    .addLocation(location, null)
                    .addMeta(event.getPlayer().getUniqueId(), UUIDType.MAINPLAYER)
            );
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
                Optional<UUIDItem> uuidItemsOptional = itemHandler.getUUIDItem(event.getCurrentItem());
                if (uuidItemsOptional.isPresent()) {
                    lastInventory.put(player, event.getClickedInventory());
                    lastHeldUUIDItem.put(player, uuidItemsOptional.get());
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
        if (event.isCancelled()) return;
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
                Optional<UUIDItem> uuidItemsOptional = itemHandler.getUUIDItem(event.getCurrentItem());
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
                UUIDItem item = lastHeldUUIDItem.get(player);
                Inventory inventory = lastInventory.get(player);
                if (inventory.getType().equals(InventoryType.PLAYER)) {
                    //The last inventory is player, so not actually retrieving
                    lastHeldUUIDItem.remove(player);
                    lastInventory.remove(player);
                    return;
                }
                Location location = inventory.getLocation();
                assert location != null;
                UUID logID = log.info("{} retrieved ({}) from {} at ({}, {}, {}) in {}",
                        player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_RETREIVE, item.getUuid())
                        .addLocation(location, Objects.requireNonNull(inventory).getType().name())
                        .addMeta(event.getPlayer().getUniqueId(), UUIDType.MAINPLAYER)
                );

                lastHeldUUIDItem.remove(player);
                lastInventory.remove(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(ClientJoinEvent event) {
        Location location = event.getPlayer().getLocation();
        itemHandler.getUUIDItems(event.getPlayer()).forEach(uuidItem -> {
            UUID logUUID = log.info("{} Logged in with ({}) at ({}, {}, {}) in {}",
                    event.getPlayer().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_LOGIN, uuidItem.getUuid())
                    .addLocation(location, null)
                    .addMeta(event.getPlayer().getUniqueId(), UUIDType.MAINPLAYER)
            );
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
                itemHandler.getUUIDItem(event.getCurrentItem()).ifPresent(uuidItem -> {
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
            if (itemHandler.getUUIDItem(event.getCursor()).isPresent()) {
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
                if (itemHandler.getUUIDItem(event.getItem()).isPresent()) {
                    //prevent using an UUIDItem in a Jukebox
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        itemHandler.getUUIDItem(event.getEntity().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getEntity().getLocation();
            UUID logUUID = log.info("{} despawned at ({}, {}, {}) in {}",
                    uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_DESPAWN, uuidItem.getUuid())
                    .addLocation(location, null)
            );
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMoveEvent(InventoryMoveItemEvent event) {
        itemHandler.getUUIDItem(event.getItem()).ifPresent(uuidItem -> {
            Location locationSource = event.getSource().getLocation();
            Location locationDestination = event.getDestination().getLocation();
            assert locationSource != null;
            assert locationDestination != null;
            UUID logUUID = log.info("({}) moved from {} ({}, {}, {}) to {} ({}, {}, {}) in {}",
                    uuidItem.getUuid(),
                    event.getSource().getType().toString(), locationSource.getBlockX(), locationSource.getBlockY(), locationSource.getBlockZ(),
                    event.getDestination().getType().toString(), locationDestination.getBlockX(), locationDestination.getBlockY(), locationDestination.getBlockZ(),
                    locationDestination.getWorld().getName()
            );
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_INVENTORY_MOVE, uuidItem.getUuid())
                    .addLocation(locationDestination, event.getDestination().getType().toString())
            );
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickupEvent(InventoryPickupItemEvent event) {
        itemHandler.getUUIDItem(event.getItem().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getInventory().getLocation();
            assert location != null;
            UUID logUUID = log.info("({}) was picked up by block {} at ({}, {}, {}) in {}",
                    uuidItem.getUuid(), event.getInventory().getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_INVENTORY_PICKUP, uuidItem.getUuid())
                    .addLocation(location, event.getInventory().getType().name())
            );
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDropItemEvent(BlockDropItemEvent event) {
        if (event.isCancelled()) return;
        event.getItems().forEach(item -> {
            itemHandler.getUUIDItem(item.getItemStack()).ifPresent(uuidItem -> {
                Location location = event.getBlock().getLocation();
                UUID logUUID = log.info("{} caused ({}) to be dropped from block {} at ({}, {}, {}) in {}",
                        event.getPlayer().getName(), uuidItem.getUuid(), event.getBlockState().getType().name(),
                        location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()
                );
                uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_CONTAINER_BREAK, uuidItem.getUuid())
                        .addLocation(location, event.getBlockState().getType().name())
                        .addMeta(event.getPlayer().getUniqueId(), UUIDType.MAINPLAYER)
                );
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
        itemHandler.getUUIDItem(event.getItem()).ifPresent(uuidItem -> {
            Location location = event.getBlock().getLocation();
            UUID logUUID = log.info("({}) was dispensed from block {} at ({}, {}, {}) in {}",
                    uuidItem.getUuid(), event.getBlock().getType().name(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()
            );
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_BLOCK_DISPENSE, uuidItem.getUuid())
                    .addLocation(location, event.getBlock().getType().name())
            );
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplore(BlockExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (block.getState() instanceof Container container) {
                container.getInventory().forEach(itemStack -> {
                    itemHandler.getUUIDItem(itemStack).ifPresent(uuidItem -> {
                        Location location = container.getLocation();
                        UUID logUUID = log.info("($s) was dropped due to explosion from {} at ({}, {}, {}) in {}",
                                uuidItem.getUuid(), container.getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()
                        );
                        uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_CONTAINER_EXPLODE, uuidItem.getUuid())
                                .addLocation(location, container.getType().name())
                        );
                    });
                });
            }
        });
    }

    @UpdateEvent(delay = (long) (1 * 1000 * UUID_CHECK_TIME_SECONDS))
    public void checkPlayers() {
        log.info("Checking Players");
        uuidSet.clear();
        AtomicInteger duplicates = new AtomicInteger();
        for (Player player : Bukkit.getOnlinePlayers()) {
            itemHandler.getUUIDItems(player).forEach(uuidItem -> {
                if (!uuidSet.add(uuidItem.getUuid())) {
                    Component component = UtilMessage.deserialize("<red>WARNING</red> Potential duplicate UUID found in ")
                            .append(UtilMessage.deserialize("<yellow>{}</yellow>", player.getName())
                                    .clickEvent(ClickEvent.runCommand("/search player " + player.getName()))
                                    .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to search by Player"))))
                            .appendSpace()
                            .append(UtilMessage.deserialize("<light_purple>{}</light_purple>", uuidItem.getUuid().toString())
                                    .clickEvent(ClickEvent.runCommand("/search item " + uuidItem.getUuid().toString()))
                                    .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to search by UUID"))))
                            .append(UtilMessage.deserialize(" (<green>{}</green>)", uuidItem.getIdentifier()));
                    clientManager.sendMessageToRank("Core", component, Rank.HELPER);
                    log.info("Potential duplicate ({}) found in player {}", uuidItem.getUuid(), player.getUniqueId());
                    duplicates.getAndIncrement();
                }
            });
        }
        log.info("Checking {} players for UUIDItem duplicates, found {}", Bukkit.getOnlinePlayers().size(), duplicates.get());
    }

    private void processExit(Player player) {
        if (lastHeldUUIDItem.containsKey(player) && lastInventory.containsKey(player)) {
            //the player is holding an item, and the inventory has closed. This means they have the item.
            UUIDItem item = lastHeldUUIDItem.get(player);
            Inventory inventory = lastInventory.get(player);
            if (inventory.getType().equals(InventoryType.PLAYER)) {
                //The last inventory is player, so not actually retrieving
                lastHeldUUIDItem.remove(player);
                lastInventory.remove(player);
                return;
            }
            Location location = inventory.getLocation();
            assert location != null;
            UUID logID = log.info("({}) dropped due to disconnect ({}) from {} at ({}, {}, {}) in {}",
                    player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_DROP, item.getUuid())
                    .addLocation(location, Objects.requireNonNull(inventory).getType().name())
                    .addMeta(player.getUniqueId(), UUIDType.MAINPLAYER)
            );

            lastHeldUUIDItem.remove(player);
            lastInventory.remove(player);
        }
        Location location = player.getLocation();
        itemHandler.getUUIDItems(player).forEach(uuidItem -> {
            UUID logUUID = log.info("{} Logged out with ({}) at ({}, {}, {}) in {}",
                    player.getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logUUID, UUIDLogType.ITEM_LOGOUT, uuidItem.getUuid())
                    .addLocation(location, null)
                    .addMeta(player.getUniqueId(), UUIDType.MAINPLAYER)
            );
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
            itemHandler.getUUIDItem(itemStack).ifPresent(item -> {
                Location location = inventory.getLocation();
                assert location != null;
                UUID logID = log.info("{} retrieved ({}) from {} at ({}, {}, {}) in {}",
                        player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_RETREIVE, item.getUuid())
                        .addLocation(location, Objects.requireNonNull(inventory).getType().name())
                        .addMeta(player.getUniqueId(), UUIDType.MAINPLAYER)
                );
            });

        }
    }

    private void processStoreItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items
            itemHandler.getUUIDItem(itemStack).ifPresent(item -> {
                Location location = inventory.getLocation();
                assert location != null;
                UUID logID = log.info("{} stored ({}) in {} at ({}, {}, {}) in {}",
                        player.getName(), item.getUuid(), inventory.getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                uuidManager.getUuidRepository().getUuidLogger().addItemLog((ItemLog) new ItemLog(logID, UUIDLogType.ITEM_CONTAINER_STORE, item.getUuid())
                        .addLocation(location, Objects.requireNonNull(inventory).getType().name())
                        .addMeta(player.getUniqueId(), UUIDType.MAINPLAYER)
                );
            });
        }
    }

    private void processStoreItemInSlot(Player player, Inventory inventory, int slot) {
        processStoreItem(player, inventory, inventory.getItem(slot));
    }

}
