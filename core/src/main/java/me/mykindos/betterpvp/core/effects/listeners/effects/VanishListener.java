package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

@BPvPListener
@Singleton
public class VanishListener implements Listener {
    private final EffectManager effectManager;

    @Inject
    public VanishListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        effectManager.getAllEntitiesWithEffects().stream()
                .filter(livingEntity -> effectManager.hasEffect(livingEntity, EffectTypes.VANISH))
                .forEach(livingEntity -> event.getPlayer().hideEntity(JavaPlugin.getPlugin(Core.class), livingEntity));

    }
}
