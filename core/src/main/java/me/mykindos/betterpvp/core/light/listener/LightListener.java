package me.mykindos.betterpvp.core.light.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.light.LightManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@BPvPListener
@Singleton
public class LightListener implements Listener {

    private final Core core;
    private final LightManager lightManager;


    @Inject
    public LightListener(Core core, LightManager lightManager) {
        this.core = core;
        this.lightManager = lightManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        lightManager.updateLight(event.getPlayer().getUniqueId(), event.getTo(), event.getFrom());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lightManager.removeAllLights(event.getPlayer().getUniqueId());
    }

}
