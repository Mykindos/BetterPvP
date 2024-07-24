package me.mykindos.betterpvp.shops.shops.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.shops.events.ShopKeeperSpawnEvent;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.IShopkeeper;
import me.mykindos.betterpvp.shops.shops.utilities.ShopsNamespacedKeys;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

@PluginAdapter("MythicMobs")
@Singleton
@BPvPListener
public class MythicMobShopkeeperListener implements Listener {

    private final ShopkeeperManager shopkeeperManager;

    @Inject
    public MythicMobShopkeeperListener(ShopkeeperManager shopkeeperManager) {
        this.shopkeeperManager = shopkeeperManager;
    }

    @EventHandler
    public void onMythicShopkeeper(ShopKeeperSpawnEvent event) {
        String mythicMob = event.getShopkeeperType();

        ActiveMob activeMob = MythicBukkit.inst().getMobManager().spawnMob(mythicMob, event.getLocation());

        activeMob.getEntity().getBukkitEntity().getPersistentDataContainer().set(ShopsNamespacedKeys.SHOPKEEPER,  PersistentDataType.BOOLEAN,true);
        shopkeeperManager.addObject(activeMob.getEntity().getUniqueId(), new IShopkeeper() {
            @Override
            public Entity getEntity() {
                return activeMob.getEntity().getBukkitEntity();
            }

            @Override
            public String getShopkeeperName() {
                return PlainTextComponentSerializer.plainText().serialize(event.getName());
            }
        });
    }


}
