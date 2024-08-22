package me.mykindos.betterpvp.clans.auctionhouse;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.IAuctionDeliveryService;

import java.util.Optional;
import java.util.UUID;

public class ClansAuctionDeliveryService implements IAuctionDeliveryService {

    private final ClanManager clanManager;

    public ClansAuctionDeliveryService(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public boolean deliverAuction(UUID target, Auction auction) {

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(target);
        if(clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            clan.getCore().getMailbox().getContents().add(auction.getItemStack());
            clanManager.getRepository().updateClanMailbox(clan);
            return true;
        }

        return false;
    }

    @Override
    public boolean deliverCurrency(UUID target, int amount) {
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(target);
        if(clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            clan.saveProperty(ClanProperty.BALANCE, clan.getBalance() + amount);
            clan.messageClan("<gray>Your clan has received <green>$" + UtilFormat.formatNumber(amount) + " <gray>from an auction sale.", null, true);
            clan.messageClan("<gray>This money can be withdrawn using <yellow>/clan bank withdraw <amount>", null, true);
            return true;
        }

        return false;
    }
}
