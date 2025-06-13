package me.mykindos.betterpvp.shops.auctionhouse.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionTransaction;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

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

        String query = "SELECT * FROM auctions LEFT JOIN auction_transaction_history on auctions.id = auction_transaction_history.AuctionID WHERE Server = ? AND Season = ?";

        Statement statement = new Statement(query,
                StringStatementValue.of(Core.getCurrentServer()),
                StringStatementValue.of(Core.getCurrentSeason()));
        try (ResultSet result = database.executeQuery(statement, TargetDatabase.GLOBAL).join()) {
            while (result.next()) {
                boolean delivered = result.getBoolean(8);
                if (delivered) continue;

                UUID auctionID = UUID.fromString(result.getString(1));
                UUID seller = UUID.fromString(result.getString(4));
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(result.getString(5)));
                int sellPrice = result.getInt(6);
                long expiry = result.getLong(7);
                boolean sold = result.getBoolean(8);
                boolean cancelled = result.getBoolean(9);

                Auction auction = new Auction(auctionID, seller, item);
                auction.setSellPrice(sellPrice);
                auction.setExpiryTime(expiry);
                auction.setSold(sold);
                auction.setCancelled(cancelled);

                String buyer = result.getString(14);
                if(buyer != null) {
                    UUID buyerUUID = UUID.fromString(buyer);
                    auction.setTransaction(new AuctionTransaction(auctionID, buyerUUID));
                }

                auctions.add(auction);
            }
        } catch (SQLException ex) {
            log.error("Failed to load active auctions", ex).submit();
        }

        return auctions;
    }

    @Override
    public void save(Auction auction) {
        String query = "INSERT INTO auctions (id, Server, Season, Gamer, Item, Price, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?)";
        ItemStack itemStack = auction.getItemStack().clone();
        Statement statement = new Statement(query,
                new UuidStatementValue(auction.getAuctionID()),
                StringStatementValue.of(Core.getCurrentServer()),
                StringStatementValue.of(Core.getCurrentSeason()),
                new UuidStatementValue(auction.getSeller()),
                new StringStatementValue(Base64.getEncoder().encodeToString(itemStack.serializeAsBytes())),
                new IntegerStatementValue(auction.getSellPrice()),
                new LongStatementValue(auction.getExpiryTime()));

        database.executeUpdateAsync(statement, TargetDatabase.GLOBAL);
    }

    public void saveAuctionTransaction(UUID buyer, Auction auction) {
        String query = "INSERT INTO auction_transaction_history (AuctionID, Buyer, TimeSold) VALUES (?, ?, ?)";
        Statement statement = new Statement(query,
                new UuidStatementValue(auction.getAuctionID()),
                new UuidStatementValue(buyer),
                new TimestampStatementValue(Instant.now()));

        database.executeUpdateAsync(statement, TargetDatabase.GLOBAL);
    }

    public void setExpired(Auction auction, boolean expired) {
        String query = "UPDATE auctions SET Expired = ? WHERE id = ?";
        Statement statement = new Statement(query,
                new BooleanStatementValue(expired),
                new UuidStatementValue(auction.getAuctionID()));

        database.executeUpdateAsync(statement, TargetDatabase.GLOBAL);
    }

    public void setSold(Auction auction, boolean sold) {
        String query = "UPDATE auctions SET Sold = ? WHERE id = ?";
        Statement statement = new Statement(query,
                new BooleanStatementValue(sold),
                new UuidStatementValue(auction.getAuctionID()));

        database.executeUpdateAsync(statement, TargetDatabase.GLOBAL);
    }

    public void setCancelled(Auction auction, boolean cancelled) {
        String query = "UPDATE auctions SET Cancelled = ? WHERE id = ?";
        Statement statement = new Statement(query,
                new BooleanStatementValue(cancelled),
                new UuidStatementValue(auction.getAuctionID()));

        database.executeUpdateAsync(statement, TargetDatabase.GLOBAL);
    }

    public void setDelivered(Auction auction, boolean delivered) {
        String query = "UPDATE auctions SET Delivered = ? WHERE id = ?";
        Statement statement = new Statement(query,
                new BooleanStatementValue(delivered),
                new UuidStatementValue(auction.getAuctionID()));

        database.executeUpdateAsync(statement, TargetDatabase.GLOBAL);
    }
}
