package me.mykindos.betterpvp.shops.auctionhouse.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionTransaction;
import org.bukkit.inventory.ItemStack;
import org.jooq.DSLContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.shops.database.jooq.Tables.AUCTIONS;
import static me.mykindos.betterpvp.shops.database.jooq.Tables.AUCTION_TRANSACTION_HISTORY;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;

@Singleton
@CustomLog
public class AuctionRepository implements IRepository<Auction> {

    private final Database database;

    @Inject
    public AuctionRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<Auction> getAll() {
        List<Auction> auctions = new ArrayList<>();

        try {
            DSLContext ctx = database.getDslContext();

            var results = ctx.select()
                    .from(AUCTIONS)
                    .innerJoin(CLIENTS)
                    .on(AUCTIONS.CLIENT.eq(CLIENTS.ID))
                    .leftJoin(AUCTION_TRANSACTION_HISTORY)
                    .on(AUCTIONS.ID.eq(AUCTION_TRANSACTION_HISTORY.AUCTION_ID))
                    .leftJoin(CLIENTS.as("buyer_clients"))
                    .on(AUCTION_TRANSACTION_HISTORY.BUYER.eq(CLIENTS.as("buyer_clients").ID))
                    .where(AUCTIONS.REALM.eq(Core.getCurrentRealm()))
                    .fetch();

            for (var result : results) {
                boolean delivered = result.get(AUCTIONS.DELIVERED);
                if (delivered) continue;

                Long auctionID = result.get(AUCTIONS.ID);
                UUID seller = UUID.fromString(result.get(CLIENTS.UUID));
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(result.get(AUCTIONS.ITEM)));
                int sellPrice = result.get(AUCTIONS.PRICE);
                long expiry = result.get(AUCTIONS.EXPIRY);
                boolean sold = result.get(AUCTIONS.SOLD);
                boolean cancelled = result.get(AUCTIONS.CANCELLED);

                Auction auction = new Auction(auctionID, seller, item);
                auction.setSellPrice(sellPrice);
                auction.setExpiryTime(expiry);
                auction.setSold(sold);
                auction.setCancelled(cancelled);

                String buyer = result.get(CLIENTS.as("buyer_clients").UUID);
                if (buyer != null) {
                    UUID buyerUUID = UUID.fromString(buyer);
                    auction.setTransaction(new AuctionTransaction(auctionID, buyerUUID));
                }

                auctions.add(auction);
            }

            return auctions;
        } catch (Exception ex) {
            log.error("Failed to load active auctions", ex).submit();
        }

        return auctions;
    }

    @Override
    public void save(Auction auction) {
        ItemStack itemStack = auction.getItemStack().clone();

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.insertInto(AUCTIONS)
                    .set(AUCTIONS.ID, auction.getAuctionID())
                    .set(AUCTIONS.REALM, Core.getCurrentRealm())
                    .set(AUCTIONS.ITEM, Base64.getEncoder().encodeToString(itemStack.serializeAsBytes()))
                    .set(AUCTIONS.PRICE, auction.getSellPrice())
                    .set(AUCTIONS.EXPIRY, auction.getExpiryTime())
                    .set(AUCTIONS.SOLD, auction.isSold())
                    .set(AUCTIONS.CANCELLED, auction.isCancelled())
                    .set(AUCTIONS.DELIVERED, false)
                    .execute();
        });
    }

    public void saveAuctionTransaction(UUID buyer, Auction auction) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.insertInto(AUCTION_TRANSACTION_HISTORY)
                    .set(AUCTION_TRANSACTION_HISTORY.AUCTION_ID, auction.getAuctionID())
                    .set(AUCTION_TRANSACTION_HISTORY.BUYER, ctx.select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(buyer.toString())))
                    .set(AUCTION_TRANSACTION_HISTORY.TIME_SOLD, Instant.now().toEpochMilli())
                    .execute();
        });
    }

    public void setSold(Auction auction, boolean sold) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.update(AUCTIONS)
                    .set(AUCTIONS.SOLD, sold)
                    .where(AUCTIONS.ID.eq(auction.getAuctionID()))
                    .execute();
        });
    }

    public void setCancelled(Auction auction, boolean cancelled) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.update(AUCTIONS)
                    .set(AUCTIONS.CANCELLED, cancelled)
                    .where(AUCTIONS.ID.eq(auction.getAuctionID()))
                    .execute();
        });
    }

    public void setDelivered(Auction auction, boolean delivered) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.update(AUCTIONS)
                    .set(AUCTIONS.DELIVERED, delivered)
                    .where(AUCTIONS.ID.eq(auction.getAuctionID()))
                    .execute();
        });
    }
}
