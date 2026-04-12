package me.mykindos.betterpvp.shops.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.UUID;

@BPvPListener
@Singleton
@PluginAdapter("Mapper")
@PluginAdapter("ModelEngine")
public class ShopsNPCListener implements Listener {

    private final SceneObjectRegistry npcRegistry;

    @Inject
    private ShopsNPCListener(SceneObjectRegistry npcRegistry) {
        this.npcRegistry = npcRegistry;
    }

    private boolean isShopsNPC(UUID uuid) {
        return this.npcRegistry.getObject(uuid) != null;
    }

    private boolean isShopsNPC(Entity entity) {
        return this.npcRegistry.getObject(entity.getUniqueId()) != null;
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (isShopsNPC(event.getDamagee())) {
            event.cancel("Cannot damage shopkeepers");
        }
    }

    @EventHandler
    public void onCollide(ThrowableHitEntityEvent event) {
        if (isShopsNPC(event.getCollision())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCatch(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            if (event.getCaught() == null) return;
            if (isShopsNPC(event.getCaught())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFetchEntity(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(entity -> {
            return isShopsNPC(entity.getKey());
        });
    }

}
