package me.mykindos.betterpvp.clans.auctionhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@BPvPListener
@PluginAdapter("Shops")
public class ClansAuctionListener implements Listener {

    private final ClanManager clanManager;
    private final AuctionManager auctionManager;

    @Inject
    public ClansAuctionListener(ClanManager clanManager) {
        this.clanManager = clanManager;
        this.auctionManager = JavaPlugin.getPlugin(Shops.class).getInjector().getInstance(AuctionManager.class);
        this.auctionManager.setDeliveryService(new ClansAuctionDeliveryService(clanManager));
    }

}
