package me.mykindos.betterpvp.core.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@Slf4j
public class UUIDLogger {

    private static Database database;

    @Inject
    public UUIDLogger(Database database) {
        UUIDLogger.database = database;
    }

    /**
     *
     * @param id - the UUID of the log this information is about
     * @param itemUuid - the UUID of the item this information is about
     * @param type - the type of log this is
     * @param uuid - UUID of the player
     */
    public static void addItemUUIDMetaInfoPlayer(UUID id, UUID itemUuid, UUIDLogType type, @Nullable UUID uuid) {
        addItemUUIDMetaInfo(id, itemUuid, type, uuid, UUIDType.PLAYER);
    }

    /**
     *
     * @param id - the UUID of the log this information is about
     * @param itemUuid - the UUID of the item this information is about
     * @param type - the type of log this is
     */
    public static void addItemUUIDMetaInfoNone(UUID id, UUID itemUuid, UUIDLogType type) {
        addItemUUIDMetaInfo(id, itemUuid, type, null, UUIDType.NONE);
    }


    /**
     *
     * @param logUUID - the UUID of the log this information is about
     * @param itemUuid - the UUID of the item this information is about
     * @param type - the type of log this is
     * @param uuid - UUID of type uuidType
     * @param uuidType - the type of UUID uuid is.
     */
    public static void addItemUUIDMetaInfo(@NotNull UUID logUUID, @NotNull UUID itemUuid, @NotNull UUIDLogger.UUIDLogType type, @Nullable UUID uuid, @NotNull UUIDType uuidType) {
        UUID metaUUID = UUID.randomUUID();


        String query = "INSERT INTO uuidlogmeta (id, LogUUID, ItemUUID, Type, UUID, UUIDtype) VALUES (?, ?, ?, ?, ?, ?)";
        database.executeUpdate(new Statement(query,
                new UuidStatementValue(metaUUID),
                new UuidStatementValue(logUUID),
                new UuidStatementValue(itemUuid),
                new StringStatementValue(type.name()),
                new StringStatementValue(uuid == null ? null : uuid.toString()),
                new StringStatementValue(uuidType.name())
                )
        );
    }

    /**
     *
     * @param itemUUID the uuid of the item
     * @param amount the number of logs to retrieve
     * @return A list of the last amount of logs relating to this uiid
     */
    public static List<String> getUuidLogs(UUID itemUUID, int amount) {
        List<String> logList = new ArrayList<>();
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
                logList.add("<green>" + UtilTime.getTime((System.currentTimeMillis() - time), 2) + " ago</green> " + result.getString(2));
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
    public static List<String> getPlayerLogs(UUID playerUuid, int amount) {
        List<String> logList = new ArrayList<>();
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
                logList.add("<green>" + UtilTime.getTime((System.currentTimeMillis() - time), 2) + " ago</green>  " + result.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Failed to get player logs", ex);
        }
        return logList;
    }

    public enum UUIDType {
        PLAYER,
        NONE
    }

     public enum UUIDLogType {
         /**
          * An UUIDItem is spawned
           */
         SPAWN,
        /**
          * Someone kills  of the UUIDItem holder
          */
         KILL,
         /**
          * Someone participates in the kill of an UUIDItem holder
          */
         CONTRIBUTOR,
         /**
          * UUIDItem holder dies
          */
         DEATH,
         /**
          * UUIDItem holder dies to a player
          */
         DEATH_PLAYER,
         /**
          * An UUIDItem is picked up or moved from an inventory
          */
         RETREIVE,
         /**
          * An UUIDItem is stored in a container
          */
         CONTAINER_STORE,
         /**
          * An UUIDItem is dropped due to a container being destroyed
          */
         CONTAINER_BREAK,
         /**
          * An UUIDItem is picked up by an inventory
          */
         INVENTORY_PICKUP,
         /**
          * An UUIDItem is moved by one inventory to another
          */
         INVENTORY_MOVE,
         /**
          * An UUIDItem is dispensed from a block
          */
         BLOCK_DISPENSE,
         /**
          * An UUIDItem holder logs out
          */
         LOGOUT,
         /**
          * An UUIDItem holder logs in
          */
         LOGIN,
         /**
          * An UUIDItem holder drops their UUIDItem
          */
         DROP,
         /**
          * A player pick up an UUIDItem
          */
         PICKUP,
         /**
          * An UUIDItem despawns
          */
         DESPAWN,
         /**
          * A custom, manual generated log
          */
         CUSTOM

    }
}
