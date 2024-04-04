package me.mykindos.betterpvp.core.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.formatted.item.BlockDispenseItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.ContainerBreakItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.ContainerExplodeItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.ContainerStoreItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.DeathItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.DeathPlayerItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.DespawnItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.DropItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.FormattedItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.InventoryMoveItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.InventoryPickupItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.LoginItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.LogoutItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.PickupItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.RetrieveItemLog;
import me.mykindos.betterpvp.core.logging.type.formatted.item.SpawnItemLog;
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

    private final Database database;

    @Inject
    public UUIDLogger(Database database) {
        this.database = database;
    }

    public void addItemLog(ItemLog itemLog) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            database.executeUpdate(itemLog.getLogTimeStatetment());
            database.executeBatch(itemLog.getStatements(), true);
            if (itemLog.getLocationStatement() != null) {
                database.executeUpdate(itemLog.getLocationStatement());
            }
        });
    }

    /**
     *
     * @param itemUUID the uuid of the item
     * @return A list of the last amount of logs relating to this uiid
     */
    public List<FormattedItemLog> getUuidLogs(UUID itemUUID) {
        List<FormattedItemLog> logList = new ArrayList<>();

        String query = "CALL GetUuidLogsByUuid(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                new UuidStatementValue(itemUUID)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                UUIDLogType type = UUIDLogType.valueOf(result.getString(2));
                String itemID = result.getString(3);
                String player1ID = result.getString(4);
                String player2ID = result.getString(5);
                String name = result.getString(6);
                String worldID = result.getString(7);
                int x = result.getInt(8);
                int y = result.getInt(9);
                int z = result.getInt(10);

                logList.add(formattedLogFromRow(time, type, itemID, player1ID, player2ID, name, worldID, x, y, z));
            }
        } catch (SQLException ex) {
            log.error("Failed to get UUID logs", ex);
        }
        return logList;
    }

    /**
     *
     * @param playerUuid the uuid of the player
     * @return A list of the last amount of logs relating to this player
     */
    public List<FormattedItemLog> getPlayerLogs(UUID playerUuid) {
        List<FormattedItemLog> logList = new ArrayList<>();

        String query = "CALL GetUuidLogsByPlayer(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(playerUuid)
                )
        );

        try {
            while(result.next()) {
                long time = result.getLong(1);
                UUIDLogType type = UUIDLogType.valueOf(result.getString(2));
                String itemID = result.getString(3);
                String player1ID = result.getString(4);
                String player2ID = result.getString(5);
                String name = result.getString(6);
                String worldID = result.getString(7);
                int x = result.getInt(8);
                int y = result.getInt(9);
                int z = result.getInt(10);

                logList.add(formattedLogFromRow(time, type, itemID, player1ID, player2ID, name, worldID, x, y, z));
            }
        } catch (SQLException ex) {
            log.error("Failed to get player logs", ex);
        }
        return logList;
    }
    public FormattedItemLog formattedLogFromRow(long time, UUIDLogType type, String itemID, String player1ID, String player2ID, String name, String world, int x, int y, int z) {
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
            case ITEM_DROP -> {
                return new DropItemLog(time, item, offlinePlayer1, location);
            }
            case ITEM_DEATH -> {
                return new DeathItemLog(time, item, offlinePlayer1, location);
            }
            case ITEM_LOGIN -> {
                return new LoginItemLog(time, item, offlinePlayer1, location);
            }
            case ITEM_SPAWN -> {
                return new SpawnItemLog(time, item, offlinePlayer1, offlinePlayer2);
            }
            case ITEM_LOGOUT -> {
                return new LogoutItemLog(time, item, offlinePlayer1, location);
            }
            case ITEM_CONTAINER_EXPLODE -> {
                return new ContainerExplodeItemLog(time, item, name, location);
            }
            case ITEM_DESPAWN -> {
                return new DespawnItemLog(time, item, location);
            }
            case ITEM_RETREIVE -> {
                return new RetrieveItemLog(time, item, offlinePlayer1, name, location);
            }
            case ITEM_CONTAINER_STORE -> {
                return new ContainerStoreItemLog(time, item, offlinePlayer1, name, location);
            }
            case ITEM_DEATH_PLAYER -> {
                return new DeathPlayerItemLog(time, item, offlinePlayer1, offlinePlayer2, location);
            }
            case ITEM_BLOCK_DISPENSE -> {
                return new BlockDispenseItemLog(time, item, name, location);
            }
            case ITEM_INVENTORY_MOVE -> {
                return new InventoryMoveItemLog(time, item, name, location);
            }
            case ITEM_CONTAINER_BREAK -> {
                return new ContainerBreakItemLog(time, item, offlinePlayer1, name, location);
            }
            case ITEM_INVENTORY_PICKUP -> {
                return new InventoryPickupItemLog(time, item, name, location);
            }
            default -> {
                return new FormattedItemLog(time, type, item, offlinePlayer1, offlinePlayer2, name, location);
            }
        }
    }
}
