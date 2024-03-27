package me.mykindos.betterpvp.core.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.formatted.FormattedItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.PickupItemLog;
import me.mykindos.betterpvp.core.logging.type.logs.ItemLog;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
public class UUIDLogger {

    private static Database database;

    @Inject
    public UUIDLogger(Database database) {
        UUIDLogger.database = database;
    }

    public static void addItemLog(ItemLog itemLog) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            database.executeUpdate(itemLog.getLogTimeStatetment());
            database.executeUpdate(itemLog.getItemLogStatement());
            database.executeBatch(itemLog.getStatements(), true);
            database.executeBatch(itemLog.getLocationStatements(), true);
        });
    }

    /**
     *
     * @param itemUUID the uuid of the item
     * @param amount the number of logs to retrieve
     * @return A list of the last amount of logs relating to this uiid
     */
    public static List<FormattedItemLog> getUuidLogs(UUID itemUUID, int amount) {
        List<FormattedItemLog> logList = new ArrayList<>();
        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetUuidLogsByUuid(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                new UuidStatementValue(itemUUID),
                new IntegerStatementValue(amount)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                UUIDLogType type = UUIDLogType.valueOf(result.getString(2));
                String itemID = result.getString(4);
                String player1ID = result.getString(5);
                String player2ID = result.getString(6);
                String name = result.getString(7);
                String worldID = result.getString(8);
                int X = result.getInt(9);
                int Y = result.getInt(10);
                int Z = result.getInt(11);

                logList.add(formattedLogFromRow(time, type, itemID, player1ID, player2ID, name, worldID, X, Y, Z));
            }
        } catch (SQLException ex) {
            log.error("Failed to get UUID logs", ex);
        }
        return logList;
    }

    /**
     *
     * @param playerUuid the uuid of the player
     * @param amount the number of logs to retrieve
     * @return A list of the last amount of logs relating to this player
     */
    public static List<FormattedItemLog> getPlayerLogs(UUID playerUuid, int amount) {
        List<FormattedItemLog> logList = new ArrayList<>();
        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetUuidLogsByPlayer(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(playerUuid),
                        new IntegerStatementValue(amount)
                )
        );

        try {
            while(result.next()) {
                long time = result.getLong(1);
                UUIDLogType type = UUIDLogType.valueOf(result.getString(2));
                String itemID = result.getString(4);
                String player1ID = result.getString(5);
                String player2ID = result.getString(6);
                String name = result.getString(7);
                String worldID = result.getString(8);
                int X = result.getInt(9);
                int Y = result.getInt(10);
                int Z = result.getInt(11);

                logList.add(formattedLogFromRow(time, type, itemID, player1ID, player2ID, name, worldID, X, Y, Z));
            }
        } catch (SQLException ex) {
            log.error("Failed to get player logs", ex);
        }
        return logList;
    }
    public static FormattedItemLog formattedLogFromRow(long time, UUIDLogType type, String itemID, String player1ID, String player2ID, String name, String world, int x, int y, int z) {
        UUID item = UUID.fromString(itemID);

        OfflinePlayer offlinePlayer1 = null;
        if (player1ID != null) {
            offlinePlayer1 = Bukkit.getOfflinePlayer(UUID.fromString(player1ID));
        }
        OfflinePlayer offlinePlayer2 = null;
        if (player2ID != null) {
            offlinePlayer2 = Bukkit.getOfflinePlayer(UUID.fromString(player2ID));
        }
        Location location = null;
        if (world != null) {
            location = new Location(Bukkit.getWorld(UUID.fromString(world)), x, y, z);
        }

        switch (type) {
            case ITEM_PICKUP -> {
                return new PickupItemLog(time, item, offlinePlayer1, location);
            }
            default -> {
                return new FormattedItemLog(time, type, item, offlinePlayer1, offlinePlayer2, name, location);
            }
        }

    }
}
