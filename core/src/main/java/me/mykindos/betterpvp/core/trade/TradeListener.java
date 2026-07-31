package me.mykindos.betterpvp.core.trade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

/**
 * Drives the trade clock and ends trades that their players have walked out of.
 * <p>
 * Both handlers run at {@link EventPriority#MONITOR} while the player is still resolvable, so escrowed
 * items go back into an inventory that will still be saved.
 */
@BPvPListener
@Singleton
public class TradeListener implements Listener {

    private final TradeManager tradeManager;

    @Inject
    public TradeListener(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @UpdateEvent(delay = 250)
    public void tickTrades() {
        tradeManager.tick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        tradeManager.clear(event.getPlayer().getUniqueId(), TradeCancelReason.DISCONNECTED);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        tradeManager.clear(event.getPlayer().getUniqueId(), TradeCancelReason.DIED);
    }

    /**
     * Hands every escrow back before the server stops holding them.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() instanceof Core) {
            tradeManager.cancelAll();
        }
    }
}
