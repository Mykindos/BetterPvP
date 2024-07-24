package me.mykindos.betterpvp.shops.shops.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import me.mykindos.betterpvp.shops.shops.items.DynamicShopItem;
import me.mykindos.betterpvp.shops.shops.items.ShopItem;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.IShopkeeper;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.ParrotShopkeeper;
import me.mykindos.betterpvp.shops.shops.utilities.ShopsNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
@BPvPListener
@CustomLog
public class ShopListener implements Listener {

    private final ShopkeeperManager shopkeeperManager;
    private final ShopManager shopManager;
    private final ItemHandler itemHandler;
    private final ClientManager clientManager;
    private final WeaponManager weaponManager;

    @Inject
    public ShopListener(ShopkeeperManager shopkeeperManager, ShopManager shopManager, ItemHandler itemHandler, ClientManager clientManager, WeaponManager weaponManager) {
        this.shopkeeperManager = shopkeeperManager;
        this.shopManager = shopManager;
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
        this.weaponManager = weaponManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!(event.getRightClicked() instanceof LivingEntity target)) return;


        Optional<IShopkeeper> shopkeeperOptional = shopkeeperManager.getObject(target.getUniqueId());
        shopkeeperOptional.ifPresent(shopkeeper -> {
            var shopkeeperItems = shopManager.getShopItems(shopkeeper.getShopkeeperName());
            if (shopkeeperItems == null || shopkeeperItems.isEmpty()) return;

            var menu = new ShopMenu(Component.text(shopkeeper.getShopkeeperName()), shopkeeperItems, itemHandler, clientManager);
            menu.show(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBuyItem(PlayerBuyItemEvent event) {
        boolean isShifting = event.getClickType().name().contains("SHIFT");

        //Optional<IWeapon> weaponByItemStack = weaponManager.getWeaponByItemStack(event.getItem());
        //if(isShifting && ((weaponByItemStack.isPresent() && weaponByItemStack.get() instanceof LegendaryWeapon)
        //        || event.getItem().getItemMeta() instanceof Damageable)) {
        //    isShifting = false;
        //}

        int cost = isShifting ? event.getShopItem().getBuyPrice() * 64 : event.getShopItem().getBuyPrice();

        if (event.getCurrency() == ShopCurrency.COINS) {
            if (event.getGamer().getIntProperty(GamerProperty.BALANCE) < cost) {
                event.cancel("You have insufficient funds to purchase this item.");
                return;
            }
        } else if (event.getCurrency() == ShopCurrency.FRAGMENTS) {
            if (event.getGamer().getIntProperty(GamerProperty.FRAGMENTS) < cost) {
                event.cancel("You have insufficient funds to purchase this item.");
                return;
            }
        } else if (event.getCurrency() == ShopCurrency.BARK) {
            if (!UtilInventory.contains(event.getPlayer(), "progression:tree_bark", cost)) {
                event.cancel("You have insufficient funds to purchase this item.");
                return;
            }
        }

        if (event.getPlayer().getInventory().firstEmpty() == -1) {
            event.cancel("Your inventory is full.");
            return;
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalBuyItem(PlayerBuyItemEvent event) {
        if (event.isCancelled()) {
            UtilMessage.message(event.getPlayer(), "Shop", event.getCancelReason());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            return;
        }

        boolean isShifting = event.getClickType().name().contains("SHIFT");

        //Optional<IWeapon> weaponByItemStack = weaponManager.getWeaponByItemStack(event.getItem());
        //if(isShifting && (weaponByItemStack.isPresent() && weaponByItemStack.get() instanceof LegendaryWeapon)) {
        //    isShifting = false;
        //}

        int cost = isShifting ? event.getShopItem().getBuyPrice() * 64 : event.getShopItem().getBuyPrice();
        int amount = isShifting ? 64 : event.getShopItem().getAmount();

        if (event.getCurrency() == ShopCurrency.COINS) {
            event.getGamer().saveProperty(GamerProperty.BALANCE.name(), event.getGamer().getIntProperty(GamerProperty.BALANCE) - cost);
        } else if (event.getCurrency() == ShopCurrency.FRAGMENTS) {
            event.getGamer().saveProperty(GamerProperty.FRAGMENTS.name(), event.getGamer().getIntProperty(GamerProperty.FRAGMENTS) - cost);
        } else if (event.getCurrency() == ShopCurrency.BARK) {
            Player player = event.getPlayer();
            UtilInventory.remove(player, "progression:tree_bark", cost);
        }

        if (event.getShopItem() instanceof DynamicShopItem dynamicShopItem) {
            dynamicShopItem.setCurrentStock(Math.max(0, dynamicShopItem.getCurrentStock() - amount));
        }

        ItemStack boughtItem = new ItemStack(event.getShopItem().getMaterial(), amount);
        boughtItem.editMeta(meta -> meta.setCustomModelData(event.getShopItem().getModelData()));

        UtilItem.insert(event.getPlayer(), itemHandler.updateNames(boughtItem));
        UtilMessage.simpleMessage(event.getPlayer(), "Shop", "You have purchased <alt2>%d %s</alt2> for <alt2>%s %s</alt2>.",
                amount, event.getShopItem().getItemName(), NumberFormat.getInstance().format(cost), event.getCurrency().name().toLowerCase());
        UtilSound.playSound(event.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f, false);
        log.info("{} purchased {}x {} for {} {}",
                        event.getPlayer().getName(), amount, event.getShopItem().getItemName(), cost, event.getCurrency().name().toLowerCase())
                .setAction("SHOP_BUY").addClientContext(event.getPlayer())
                .addContext("ShopItem", event.getShopItem().getItemName()).addContext("Amount", amount + "")
                .addContext("Price", cost + "").submit();
        itemHandler.getUUIDItem(boughtItem).ifPresent(uuidItem -> {
            Player player = event.getPlayer();
            Location location = player.getLocation();
            log.info("{} purchased ({}) at {}", player.getName(), uuidItem.getUuid(),
                            UtilWorld.locationToString((location))).setAction("ITEM_BUY")
                    .addClientContext(player).addItemContext(uuidItem).addLocationContext(location).submit();
        });

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSellItem(PlayerSellItemEvent event) {
        if (event.getShopItem().getSellPrice() == 0) {
            event.cancel("You cannot sell this item.");
            return;
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinalSellItem(PlayerSellItemEvent event) {
        Player player = event.getPlayer();

        if (event.isCancelled()) {
            UtilMessage.message(event.getPlayer(), "Shop", event.getCancelReason());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
            return;
        }

        ShopItem shopItem = (ShopItem) event.getShopItem();


        boolean isShifting = event.getClickType().name().contains("SHIFT");
        int cost;
        int amount;

        if (player.getInventory().contains(event.getItem().getType())) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;
                ItemMeta itemMeta = item.getItemMeta();

                amount = isShifting ? item.getAmount() : event.getItem().getAmount();
                cost = amount * event.getShopItem().getSellPrice();

                if (item.getType() == event.getItem().getType()) {

                    if (!shopItem.getItemFlags().containsKey("IGNORE_MODELDATA")) {
                        if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() != shopItem.getModelData()) {
                            continue;
                        }
                    }

                    // Some items, such as imbued weapons, cannot be sold despite being the same type
                    if (item.getItemMeta().getPersistentDataContainer().has(ShopsNamespacedKeys.SHOP_NOT_SELLABLE)) {
                        continue;
                    }

                    if (item.getAmount() >= amount) {

                        if (item.getAmount() - amount < 1) {
                            player.getInventory().setItem(i, new ItemStack(Material.AIR));
                        } else {
                            ItemStack newStack = item.clone();
                            newStack.setAmount(item.getAmount() - amount);
                            player.getInventory().setItem(i, newStack);
                        }


                        if (event.getCurrency() == ShopCurrency.COINS) {
                            event.getGamer().saveProperty(GamerProperty.BALANCE.name(), event.getGamer().getIntProperty(GamerProperty.BALANCE) + cost);
                        }

                        if (event.getShopItem() instanceof DynamicShopItem dynamicShopItem) {
                            dynamicShopItem.setCurrentStock(Math.min(dynamicShopItem.getMaxStock(), dynamicShopItem.getCurrentStock() + amount));
                        }

                        UtilSound.playSound(event.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f, false);
                        UtilMessage.simpleMessage(event.getPlayer(), "Shop", "You have sold <alt2>%d %s</alt2> for <alt2>%s %s</alt2>.",
                                amount, event.getShopItem().getItemName(), UtilFormat.formatNumber(cost), event.getCurrency().name().toLowerCase());
                        log.info("{} sold {}x {} for {} {}", event.getPlayer().getName(), amount, event.getShopItem().getItemName(), cost, event.getCurrency().name().toLowerCase())
                                .setAction("SHOP_SELL").addClientContext(event.getPlayer())
                                .addContext("ShopItem", event.getShopItem().getItemName()).addContext("Amount", amount + "")
                                .addContext("Price", cost + "").submit();
                        itemHandler.getUUIDItem(item).ifPresent(uuidItem -> {
                            Location location = player.getLocation();
                            log.info("{} sold ({}) at {}", player.getName(), uuidItem.getUuid(),
                                            UtilWorld.locationToString((location))).setAction("ITEM_SELL")
                                    .addClientContext(player).addItemContext(uuidItem).addLocationContext(location).submit();
                        });

                        return;
                    }
                }
            }
        }
    }

    @UpdateEvent(delay = 180_000) // 3 minutes
    public void updateDynamicPrices() {
        List<DynamicShopItem> dynamicShopItems = new ArrayList<>();
        shopManager.getShopItems().values().forEach(shopItems -> {
            shopItems.forEach(shopItem -> {
                if (shopItem instanceof DynamicShopItem dynamicShopItem) {
                    dynamicShopItems.add(dynamicShopItem);
                }
            });
        });

        shopManager.getShopItemRepository().updateStock(dynamicShopItems);
    }

    @UpdateEvent(delay = 7_200_000) // 2 Hours
    public void refreshStocks() {
        shopManager.getShopItems().values().forEach(shopItems -> {
            shopItems.forEach(shopItem -> {
                if (shopItem instanceof DynamicShopItem dynamicShopItem) {
                    if (dynamicShopItem.getCurrentStock() < dynamicShopItem.getBaseStock()) {
                        dynamicShopItem.setCurrentStock((int) (dynamicShopItem.getCurrentStock() + (dynamicShopItem.getBaseStock() / 100 * 2.5)));
                    } else if (dynamicShopItem.getCurrentStock() > dynamicShopItem.getBaseStock()) {
                        dynamicShopItem.setCurrentStock((int) (dynamicShopItem.getCurrentStock() - (dynamicShopItem.getBaseStock() / 100 * 2.5)));
                    }
                }
            });
        });

        UtilMessage.simpleBroadcast("Shop", "Dynamic prices have been updated!",
                Component.text("This means that buy / sell prices on farming items have been adjusted to reflect the current market.", NamedTextColor.GRAY));
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (shopkeeperManager.getObject(event.getDamagee().getUniqueId()).isPresent()) {
            event.cancel("Cannot damage shopkeepers");
        }
    }

    @EventHandler
    public void onCollide(ThrowableHitEntityEvent event) {
        if (shopkeeperManager.getObject(event.getCollision().getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCatch(PlayerFishEvent event) {
        if(event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            if(event.getCaught() == null) return;
            if (shopkeeperManager.getObject(event.getCaught().getUniqueId()).isPresent()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFetchEntity(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(entity -> shopkeeperManager.getObject(entity.getKey().getUniqueId()).isPresent());
    }

    @EventHandler
    public void onFetchNearbyEntity(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(entity -> shopkeeperManager.getObject(entity.getKey().getUniqueId()).isPresent());
    }

    private static final Material[] MUSIC_DISC_MATERIALS = {
            Material.MUSIC_DISC_RELIC,
            Material.MUSIC_DISC_OTHERSIDE,
            Material.MUSIC_DISC_PIGSTEP
    };

    @UpdateEvent(delay = 1000)
    public void playParrotMusic() {

        Material song = MUSIC_DISC_MATERIALS[UtilMath.randomInt(MUSIC_DISC_MATERIALS.length)];
        for (var shopkeeper : shopkeeperManager.getObjects().values()) {
            if (shopkeeper instanceof ParrotShopkeeper) {
                Block block = shopkeeper.getEntity().getLocation().subtract(0, 2, 0).getBlock();
                if (block.getType() == Material.JUKEBOX) {
                    if (block.getState() instanceof Jukebox jukeboxState) {
                        if (!jukeboxState.isPlaying()) {
                            jukeboxState.setRecord(new ItemStack(song));
                            jukeboxState.update();
                        }
                    }
                }
            }
        }

    }

    private boolean loaded;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (shopkeeperManager.getObjects().isEmpty()) {
            if (!loaded) {
                UtilServer.runTaskLater(JavaPlugin.getPlugin(Shops.class), shopkeeperManager::loadShopsFromConfig, 100L);
                loaded = true;
            }
        }
    }

    @UpdateEvent(delay = 10000)
    public void checkShopkeepers() {
        if (shopkeeperManager.getObjects().values().stream().anyMatch(shopkeeper -> shopkeeper.getEntity() == null
                || shopkeeper.getEntity().isDead())) {
            shopkeeperManager.loadShopsFromConfig();
        }
    }

}
