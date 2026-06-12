package me.mykindos.betterpvp.core.item.remapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

/**
 * When a player changes their client language, item name/lore packets that were already rendered into the
 * previous locale need to be resent so {@link ItemPacketRemapper} re-localizes them. We refresh the
 * player's inventory (and any open container) on the next tick, once {@link Player#locale()} reflects the
 * new value.
 */
@BPvPListener
@Singleton
public class ItemLocaleRefreshListener implements Listener {

    private final Core core;

    @Inject
    private ItemLocaleRefreshListener(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        final Player player = event.getPlayer();
        // Defer a tick so player.locale() returns the new locale when the refreshed packets are built.
        UtilServer.runTaskLater(core, () -> {
            if (player.isOnline()) {
                // Resends the player inventory and the currently open container (WINDOW_ITEMS / SET_SLOT),
                // which pass back through ItemPacketRemapper and are re-localized for the new locale.
                player.updateInventory();
            }
        }, 1L);
    }
}
