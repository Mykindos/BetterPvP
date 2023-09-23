package me.mykindos.betterpvp.shops.shops.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerBuyItemEvent;
import me.mykindos.betterpvp.core.components.shops.events.PlayerSellItemEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import me.mykindos.betterpvp.shops.shops.menus.ShopMenu;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.IShopkeeper;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.ParrotShopkeeper;
import me.mykindos.betterpvp.shops.shops.utilities.ShopsNamespacedKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@Singleton
@BPvPListener
public class ShopListener implements Listener {

    private final ShopkeeperManager shopkeeperManager;
    private final ShopManager shopManager;
    private final ItemHandler itemHandler;

    @Inject
    public ShopListener(ShopkeeperManager shopkeeperManager, ShopManager shopManager, ItemHandler itemHandler) {
        this.shopkeeperManager = shopkeeperManager;
        this.shopManager = shopManager;
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!(event.getRightClicked() instanceof LivingEntity target)) return;


        Optional<IShopkeeper> shopkeeperOptional = shopkeeperManager.getObject(target.getUniqueId());
        shopkeeperOptional.ifPresent(shopkeeper -> {
            var menu = new ShopMenu(event.getPlayer(), Component.text(shopkeeper.getShopkeeperName()),
                    shopManager.getShopItems(shopkeeper.getShopkeeperName()), itemHandler);
            MenuManager.openMenu(event.getPlayer(), menu);
        });
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onFinalBuyItem(PlayerBuyItemEvent event) {
        if(event.isCancelled()) return;
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onFinalSellItem(PlayerSellItemEvent event) {
        if(event.isCancelled()) return;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (shopkeeperManager.getObject(event.getDamagee().getUniqueId()).isPresent()) {
            event.cancel("Cannot damage shopkeepers");
        }
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


}
