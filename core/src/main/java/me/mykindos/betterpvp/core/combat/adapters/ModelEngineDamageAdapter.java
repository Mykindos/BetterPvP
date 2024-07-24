package me.mykindos.betterpvp.core.combat.adapters;

import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.nms.entity.HitboxEntity;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.utils.serialize.Optl;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.Set;

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
    public void onFetchEntity(FetchNearbyEntityEvent<LivingEntity> event) {
        Set<LivingEntity> entityToAdd = new HashSet<>();
        Set<LivingEntity> entityToRemove = new HashSet<>();

        event.getEntities().forEach(entry -> {
            HitboxEntity hitboxEntity = ModelEngineAPI.getNMSHandler().getEntityHandler().castHitbox(entry.getKey());
            if(hitboxEntity != null) {
                ActiveMob mythicMobInstance = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entry.getKey());
                if (mythicMobInstance.getEntity().getBukkitEntity() instanceof LivingEntity newTarget) {
                    entityToAdd.add(newTarget);
                }

                entityToRemove.add(entry.getKey());
            } else {
                ActiveMob mythicMobInstance = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entry.getKey());
                if(mythicMobInstance != null) {
                    Optl<AbstractEntity> parent = mythicMobInstance.getParent();
                    if (parent.isPresent()) {
                        AbstractEntity abstractEntity = parent.get();
                        if (abstractEntity != null && abstractEntity.getBukkitEntity() instanceof LivingEntity newTarget) {
                            entityToAdd.add(newTarget);
                        }

                        entityToRemove.add(entry.getKey());
                    }
                }
            }
        });
        event.getEntities().removeIf(entity -> entityToRemove.contains(entity.getKey()));
        entityToAdd.forEach(entity -> {
            entity.customName(Component.text(UtilFormat.stripColor(entity.getName())));
            event.getEntities().add(new KeyValue<>(entity, event.getEntityProperty()));
        });
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
