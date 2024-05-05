package me.mykindos.betterpvp.shops.shops.listeners;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.shops.events.ShopKeeperSpawnEvent;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.IShopkeeper;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.inject.Inject;
import javax.inject.Singleton;

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
