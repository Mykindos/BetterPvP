package me.mykindos.betterpvp.core.combat.adapters;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.nms.entity.HitboxEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import javax.inject.Singleton;

@PluginAdapter("ModelEngine")
@PluginAdapter("MythicMobs")
@Singleton
@BPvPListener
@CustomLog
public class ModelEngineDamageAdapter implements Listener {

    /**
     *  Super important!
     *  This event is used to cancel damage events if the damagee is a model engine hitbox entity.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageHitboxEntity(PreCustomDamageEvent event) {
        if(ModelEngineAPI.getNMSHandler().getEntityHandler().castHitbox(event.getCustomDamageEvent().getDamagee()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFetchEntity(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(entity -> ModelEngineAPI.getNMSHandler().getEntityHandler().castHitbox(entity.getKey()) != null);
    }

    @EventHandler
    public void onThrowableHit(ThrowableHitEntityEvent event) {
        HitboxEntity hitboxEntity = ModelEngineAPI.getNMSHandler().getEntityHandler().castHitbox(event.getCollision());
        if(hitboxEntity != null) {
            ActiveMob mythicMobInstance = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(event.getCollision());
            if(mythicMobInstance.getEntity().getBukkitEntity() instanceof LivingEntity newTarget) {
                event.setCollision(newTarget);
            }

        }
    }

    @EventHandler
    public void entityDeath(EntityDeathEvent event) {
        HitboxEntity hitboxEntity = ModelEngineAPI.getNMSHandler().getEntityHandler().castHitbox(event.getEntity());
        if(hitboxEntity != null) {
            log.error("Hitbox entity died? {} - {} - {}", event.getEntity().getType(), event.getEntity().getUniqueId(), event.getEntity().getName()).submit();
        }
    }

}
