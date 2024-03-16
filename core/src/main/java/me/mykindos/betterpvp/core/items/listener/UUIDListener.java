package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.logging.Logger;
import me.mykindos.betterpvp.core.logging.UUIDLogger;
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

@Singleton
@BPvPListener
@Slf4j
public class UUIDListener implements Listener {


    private final Core core;

    private final ItemHandler itemHandler;

    private final ClientManager clientManager;

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
    public UUIDListener(Core core, ItemHandler itemHandler, ClientManager clientManager) {
        this.core = core;
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
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
        UUID logID = Logger.info("<yellow>%s</yellow> <green>picked</green> up <light_purple>%s</light_purple> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                event.getEntity().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        if (event.getEntity() instanceof Player player) {
            UUIDLogger.addItemUUIDMetaInfoPlayer(logID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.PICKUP, player.getUniqueId());
        } else {
            UUIDLogger.addItemUUIDMetaInfoNone(logID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.PICKUP);
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
        UUID logID = Logger.info("<yellow>%s</yellow> <red>dropped</red> <light_purple>%s</light_purple> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                event.getPlayer().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        UUIDLogger.addItemUUIDMetaInfoPlayer(logID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.DROP, event.getPlayer().getUniqueId());
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
            contributors.append(player.getName()).append("</yellow>, <yellow>");
        }
        Location location = victim.getLocation();
        for (UUIDItem item : uuidItemsList) {
            UUID logID = Logger.info("<yellow>%s</yellow> was killed while holding <light_purple>%s</light_purple> by <yellow>%s</yellow> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>, contributed by %s",
                    victim.getName(), item.getUuid(), killer.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName(), contributors);
            UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.DEATH_PLAYER, victim.getUniqueId());
            UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.KILL, killer.getUniqueId());
            for (Player player : contributions.keySet()) {
                UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.CONTRIBUTOR, player.getUniqueId());
            }
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
            UUID logID = Logger.info("<yellow>%s</yellow> <red>died</red> while holding <light_purple>%s</light_purple> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                    player.getName(), item.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.DEATH, player.getUniqueId());
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
                UUID logID = Logger.info("<yellow>%s</yellow> <green>retrieved</green> <light_purple>%s</yellow> from <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.RETREIVE, player.getUniqueId());

                lastHeldUUIDItem.remove(player);
                lastInventory.remove(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(ClientJoinEvent event) {
        Location location = event.getPlayer().getLocation();
        itemHandler.getUUIDItems(event.getPlayer()).forEach(uuidItem -> {
            UUID logUUID = Logger.info("<yellow>%s</yellow> <blue>Logged</blue> <green>in</green> with <light_purple>%s</light_purple> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                    event.getPlayer().getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.LOGOUT);
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
            UUID logUUID = Logger.info("<light_purple>%s</light_purple> <red><bold>despawned</bold></red> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                    uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.DESPAWN);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMoveEvent(InventoryMoveItemEvent event) {
        itemHandler.getUUIDItem(event.getItem()).ifPresent(uuidItem -> {
            Location locationSource = event.getSource().getLocation();
            Location locationDestination = event.getDestination().getLocation();
            assert locationSource != null;
            assert locationDestination != null;
            UUID logUUID = Logger.info("<light_purple>>%s</light_purple> moved from <aqua>%s</aqua> (<green>%s</green>, <green>%s</green>, <green>%s</green>) to <aqua>%s</aqua> (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                    uuidItem.getUuid(),
                    event.getSource().getType().toString(), locationSource.getBlockX(), locationSource.getBlockY(), locationSource.getBlockZ(),
                    event.getDestination().getType().toString(), locationDestination.getBlockX(), locationDestination.getBlockY(), locationDestination.getBlockZ(),
                    locationDestination.getWorld().getName()
            );
            UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.INVENTORY_MOVE);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickupEvent(InventoryPickupItemEvent event) {
        itemHandler.getUUIDItem(event.getItem().getItemStack()).ifPresent(uuidItem -> {
            Location location = event.getInventory().getLocation();
            assert location != null;
            UUID logUUID = Logger.info("<light_purple>%s</light_purple> was picked up by block <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                    uuidItem.getUuid(), event.getInventory().getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.INVENTORY_PICKUP);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDropItemEvent(BlockDropItemEvent event) {
        if (event.isCancelled()) return;
        event.getItems().forEach(item -> {
            itemHandler.getUUIDItem(item.getItemStack()).ifPresent(uuidItem -> {
                Location location = event.getBlock().getLocation();
                UUID logUUID = Logger.info("<yellow>%s</yellow> caused <light_purple>%s</light_purple> to be <red>dropped</red> from block <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        event.getPlayer().getName(), uuidItem.getUuid(), event.getBlockState().getType().name(),
                        location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()
                );
                UUIDLogger.addItemUUIDMetaInfoPlayer(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.CONTAINER_BREAK, event.getPlayer().getUniqueId());
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDispenseEvent(BlockDispenseEvent event) {
        itemHandler.getUUIDItem(event.getItem()).ifPresent(uuidItem -> {
            Location location = event.getBlock().getLocation();
            UUID logUUID = Logger.info("<light_purple>%s</light_purple> was <red>dispensed</red> from block <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                    uuidItem.getUuid(), event.getBlock().getType().name(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()
            );
            UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.BLOCK_DISPENSE);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplore(BlockExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (block.getState() instanceof Container container) {
                container.getInventory().forEach(itemStack -> {
                    itemHandler.getUUIDItem(itemStack).ifPresent(uuidItem -> {
                        Location location = container.getLocation();
                        UUID logUUID = Logger.info("<light_purple>$s</light_purple> was <red>dropped</red> due to explosion from <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                                uuidItem.getUuid(), container.getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()
                        );
                        UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.CONTAINER_BREAK);
                    });
                });
            }
        });
    }

    @UpdateEvent(delay = (long) (1 * 1000 * UUID_CHECK_TIME_SECONDS))
    public void checkPlayers() {
        log.info("Checking Players");
        uuidSet.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            itemHandler.getUUIDItems(player).forEach(uuidItem -> {
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
                    clientManager.sendMessageToRank("Core", component, Rank.HELPER);
                }
            });
        }
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
            UUID logID = Logger.info("<yellow>%s</yellow> <red>dropped</red> due to disconnect <light_purple>%s</yellow> from <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>", player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.DROP, player.getUniqueId());
            UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.LOGOUT, player.getUniqueId());

            lastHeldUUIDItem.remove(player);
            lastInventory.remove(player);
        }
        Location location = player.getLocation();
        itemHandler.getUUIDItems(player).forEach(uuidItem -> {
            UUID logUUID = Logger.info("<yellow>%s</yellow> <blue>Logged</blue> <red>out</red> with <light_purple>%s</light_purple> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>", player.getName(), uuidItem.getUuid(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
            UUIDLogger.addItemUUIDMetaInfoNone(logUUID, uuidItem.getUuid(), UUIDLogger.UUIDLogType.LOGOUT);
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
                UUID logID = Logger.info("<yellow>%s</yellow> <green>retrieved</green> <light_purple>%s</light_purple> from <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        player.getName(), item.getUuid(), Objects.requireNonNull(inventory).getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.RETREIVE, player.getUniqueId());
            });

        }
    }

    private void processStoreItem(Player player, Inventory inventory, ItemStack itemStack) {
        if (!INVENTORY_NO_STORE_TYPES.contains(inventory.getType())) {
            //this inventory can store items
            itemHandler.getUUIDItem(itemStack).ifPresent(item -> {
                Location location = inventory.getLocation();
                assert location != null;
                UUID logID = Logger.info("<yellow>%s</yellow> <red>stored</red> <light_purple>%s</light_purple> in <aqua>%s</aqua> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        player.getName(), item.getUuid(), inventory.getType().name(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
                UUIDLogger.addItemUUIDMetaInfoPlayer(logID, item.getUuid(), UUIDLogger.UUIDLogType.CONTAINER_STORE, player.getUniqueId());
            });
        }
    }

    private void processStoreItemInSlot(Player player, Inventory inventory, int slot) {
        processStoreItem(player, inventory, inventory.getItem(slot));
    }

}
