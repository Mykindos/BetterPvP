package me.mykindos.betterpvp.clans.auctionhouse;

import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.core.mailbox.ClanMailbox;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.IAuctionDeliveryService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

@CustomLog
public class ClansAuctionDeliveryService implements IAuctionDeliveryService {

    private final ClanManager clanManager;
    private final ItemFactory itemFactory;

    public ClansAuctionDeliveryService(ClanManager clanManager, ItemFactory itemFactory) {
        this.clanManager = clanManager;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean deliverAuction(UUID target, Auction auction) {

        if (auction.isDelivered()) {
            log.error("Tried to re-deliver an already delivered auction?").submit();
            return true;
        }

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(target);
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            ClanMailbox mailbox = clan.getCore().getMailbox();
            if (mailbox.isLocked()) {
                return false;
            }

            if (!mailbox.getContents().add(auction.getItemStack())) {
                log.error("Failed to add items to clan mailbox").submit();
                return false;
            }

            clanManager.getRepository().updateClanMailbox(clan);

            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                final ItemStack itemStack = auction.getItemStack();
                final ItemInstance instance = itemFactory.fromItemStack(itemStack).orElseThrow();
                final Component name = instance.getBaseItem().getItemNameRenderer().createName(instance);
                if (auction.isCancelled()) {
                    UtilMessage.message(player, "core.prefix.auction-house", "clans.auction.listing-cancelled", name);
                } else if (auction.hasExpired()) {
                    UtilMessage.message(player, "core.prefix.auction-house", "clans.auction.listing-expired", name);
                } else {
                    UtilMessage.message(player, "core.prefix.auction-house", "clans.auction.purchase-delivered");
                    UtilSound.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 2f, 1f, false);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean deliverCurrency(UUID target, int amount) {
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(target);
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            clan.saveProperty(ClanProperty.BALANCE, clan.getBalance() + amount);
            clan.messageClan("clans.auction.currency-received", new Object[]{Component.text("$" + UtilFormat.formatNumber(amount), NamedTextColor.GREEN)}, null, true);
            clan.messageClan("clans.auction.currency-withdraw-info", null, null, true);
            return true;
        }

        return false;
    }
}
