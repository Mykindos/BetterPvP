package me.mykindos.betterpvp.clans.auctionhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCreateEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.PlayerPrepareListingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

@Singleton
@BPvPListener
@PluginAdapter("Shops")
@CustomLog
public class ClansAuctionListener implements Listener {

    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final AuctionManager auctionManager;

    @Inject
    public ClansAuctionListener(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        this.auctionManager = JavaPlugin.getPlugin(Shops.class).getInjector().getInstance(AuctionManager.class);
        this.auctionManager.setDeliveryService(new ClansAuctionDeliveryService(clanManager));
    }

    @EventHandler
    public void onAuctionCreate(AuctionCreateEvent event) {
        if (clanManager.getClanByPlayer(event.getPlayer()).isEmpty()) {
            event.cancel("You must be in a clan to create an auction.");
        }
    }

    @EventHandler
    public void onPrepareAuction(PlayerPrepareListingEvent event) {
        if (clanManager.getClanByPlayer(event.getPlayer()).isEmpty()) {
            event.cancel("You must be in a clan to create an auction.");
        }
    }

    @EventHandler
    public void onAuctionBuy(AuctionBuyEvent event) {
        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
        if(playerClanOptional.isEmpty()) {
            event.cancel("You must be in a clan to purchase an auction.");
            return;
        }

        Optional<Clan> sellerClanOptional = clanManager.getClanByPlayer(event.getAuction().getSeller());
        if(sellerClanOptional.isEmpty()) {
            event.cancel("The seller must be in a clan to purchase this auction."); // This should never happen
            log.warn("Seller of auction " + event.getAuction().getAuctionID().toString() + " is not in a clan.").submit();
            return;
        }

        Clan playerClan = playerClanOptional.get();
        Clan sellerClan = sellerClanOptional.get();

        if(playerClan.equals(sellerClan) && !clientManager.search().online(event.getPlayer()).isAdministrating()) {
            event.cancel("You can not purchase auctions listed by your own clan.");
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKickMember(ClanKickMemberEvent event) {
        UUID member = event.getTarget().getUniqueId();
        auctionManager.cancelAllAuctions(member);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMemberLeave(MemberLeaveClanEvent event) {
        UUID member = event.getPlayer().getUniqueId();
        auctionManager.cancelAllAuctions(member);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisband(ClanDisbandEvent event) {
        Clan clan = event.getClan();
        clan.getMembers().forEach(member -> {
            auctionManager.cancelAllAuctions(UUID.fromString(member.getUuid()));
        });
    }

}
