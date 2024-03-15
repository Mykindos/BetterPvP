package me.mykindos.betterpvp.core.effects.listeners.effects;

import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@BPvPListener
@Singleton
public class StunListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public StunListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedPosition()) {
            if (effectManager.hasEffect(event.getPlayer(), EffectTypes.STUN)) {
                event.setCancelled(true);
            }
        }
    }

}
