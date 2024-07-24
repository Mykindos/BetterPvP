package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ImmuneListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public ImmuneListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onImmuneToNegativity(EffectReceiveEvent event) {
        EffectType type = event.getEffect().getEffectType();
        if (type.isNegative()) {
            if (effectManager.hasEffect(event.getTarget(), EffectTypes.IMMUNE)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReceiveImmuneToEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) return;
        if (event.getEffect().getEffectType() == EffectTypes.IMMUNE) {
            effectManager.removeNegativeEffects(event.getTarget());
        }
    }

}
