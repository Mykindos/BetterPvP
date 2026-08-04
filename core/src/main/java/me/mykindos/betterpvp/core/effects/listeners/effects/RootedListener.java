package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class RootedListener implements Listener {

    /**
     * Tagged on every velocity this listener kills, so anything that wants to carve out an exception
     * for its own flavour of root can recognise the cancellation as ours and undo it.
     */
    public static final String CANCEL_REASON = "Rooted";

    private final EffectManager effectManager;

    @Inject
    private RootedListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(CustomEntityVelocityEvent event) {
        final Entity rawEntity = event.getEntity();
        if (!(rawEntity instanceof LivingEntity entity)) return;

        if (effectManager.hasEffect(entity, EffectTypes.ROOTED)) {
            event.cancel(CANCEL_REASON);
        }
    }
}
