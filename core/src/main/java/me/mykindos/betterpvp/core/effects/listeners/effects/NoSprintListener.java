package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

@Singleton
@BPvPListener
public class NoSprintListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public NoSprintListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        if (effectManager.hasEffect(event.getPlayer(), EffectTypes.NO_SPRINT)) {
            event.getPlayer().setSprinting(false);
        }
    }
}
