package me.mykindos.betterpvp.clans.auctionhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.events.ClanCoreDestroyedEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCancelEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCreateEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.PlayerPrepareListingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

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
        Optional<Clan> clanByPlayer = clanManager.getClanByPlayer(event.getPlayer());
        if (clanByPlayer.isEmpty()) {
            event.cancel("You must be in a clan to create an auction.");
        } else if(clanManager.getPillageHandler().isBeingPillaged(clanByPlayer.get())) {
            event.cancel("You cannot create auctions during a pillage.");
        }
    }

    @EventHandler
    public void onPrepareAuction(PlayerPrepareListingEvent event) {
        Optional<Clan> clanByPlayerOptional = clanManager.getClanByPlayer(event.getPlayer());
        if (clanByPlayerOptional.isEmpty()) {
            event.cancel("You must be in a clan to create an auction.");
            return;
        }


        Optional<Clan> clanByLocationOptional = clanManager.getClanByLocation(event.getPlayer().getLocation());
        if (clanByLocationOptional.isEmpty()) {
            event.cancel("You must be at in your clan territory or a safe zone to create an auction.");
            return;
        }

        Clan clan = clanByPlayerOptional.get();
        Clan locationClan = clanByLocationOptional.get();
        if(!clan.equals(locationClan) && !locationClan.isSafe()) {
            event.cancel("You must be at in your clan territory or a safe zone to create an auction.");
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

    @EventHandler
    public void onBuy(AuctionBuyEvent event) {
        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> {
            if(clan.getCore().getMailbox().isLocked()) {
                event.cancel("Could not buy this auction as your clan mailbox is in use.");
            }
        });
    }

    @EventHandler
    public void onCancel(AuctionCancelEvent event) {
        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> {
            if(clan.getCore().getMailbox().isLocked()) {
                event.cancel("Could not cancel this auction as your clan mailbox is in use.");
            }
        });
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onCoreDestroy(ClanCoreDestroyedEvent event) {
        event.getClan().getMembers().forEach(member -> {
            auctionManager.cancelAllAuctions(UUID.fromString(member.getUuid()));
        });
    }

}
