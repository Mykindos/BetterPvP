package me.mykindos.betterpvp.core.menu.quick;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

@BPvPListener
@Singleton
public class QuickMenuGameModeListener implements Listener {

    private final BPvPPlugin plugin;

    @Inject
    private QuickMenuGameModeListener(Core core) {
        this.plugin = core;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        final Player player = event.getPlayer();
        // The event fires before the gamemode is applied, so resend next tick once
        // QuickMenu.shouldDisplay reflects the new mode and the buttons are no longer injected.
        UtilServer.runTaskLater(plugin, player::updateInventory, 1L);
    }
}
