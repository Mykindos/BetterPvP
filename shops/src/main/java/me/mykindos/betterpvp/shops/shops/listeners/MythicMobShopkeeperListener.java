package me.mykindos.betterpvp.shops.shops.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.shops.events.ShopKeeperDespawnEvent;
import me.mykindos.betterpvp.shops.shops.events.ShopKeeperSpawnEvent;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.IShopkeeper;
import me.mykindos.betterpvp.shops.shops.utilities.ShopsNamespacedKeys;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

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
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getChunk().getWorld();
        if(world.getName().equals("world") || world.getName().equals("bossworld")) {
            Arrays.stream(event.getChunk().getEntities()).forEach(ent -> {
                if (ent.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {

                    if (MythicBukkit.inst().getMobManager().isMythicMob(ent) && ModelEngineAPI.getModeledEntity(ent) == null) {
                        ent.remove();
                    }
                }
            });
        }
    }


    @EventHandler
    public void onMythicShopkeeper(ShopKeeperSpawnEvent event) {
        String mythicMob = event.getShopkeeperType();

        ActiveMob activeMob = MythicBukkit.inst().getMobManager().spawnMob(mythicMob, event.getLocation());
        if (activeMob != null) {
            activeMob.getEntity().getBukkitEntity().getPersistentDataContainer().set(ShopsNamespacedKeys.SHOPKEEPER, PersistentDataType.BOOLEAN, true);
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

    @EventHandler
    public void onShopkeeperDespawn(ShopKeeperDespawnEvent event) {
        ActiveMob activeMob = MythicBukkit.inst().getMobManager().getMythicMobInstance(event.getEntity());
        if (activeMob != null) {
            activeMob.setDespawned();
            MythicBukkit.inst().getMobManager().unregisterActiveMob(activeMob);
        }

    }


}
