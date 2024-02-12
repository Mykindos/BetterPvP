package me.mykindos.betterpvp.core.combat.adapters;

import com.ticxo.modelengine.api.ModelEngineAPI;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Singleton;

@PluginAdapter("ModelEngine")
@Singleton
@BPvPListener
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

}
