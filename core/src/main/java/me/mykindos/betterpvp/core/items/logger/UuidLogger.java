package me.mykindos.betterpvp.core.items.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Slf4j
public class UuidLogger {

    private static Database database;

    @Inject
    public UuidLogger(Database database) {
        UuidLogger.database = database;
    }

    public static void AddUUIDMetaInfo(int id, UUID uuid, UuidLogType type, @Nullable UUID Player) {

        String query = "INSERT INTO uuidlogmeta (logId, UUID, Type, Player) VALUES (?, ?, ?, ?)";
        database.executeUpdate(new Statement(query,
                new IntegerStatementValue(id),
                new UuidStatementValue(uuid),
                new StringStatementValue(type.name()),
                new StringStatementValue(Player == null ? null : Player.toString())
                )
        );
    }

    /**
     *
     * @param uuid the uuid of the legend
     * @param amount the number of logs to retrieve
     * @return A list of the last amount of logs relating to this uiid
     */
    public static List<String> getUuidLogs(UUID uuid, int amount) {
        List<String> logList = new ArrayList<>();
        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetUuidLogsByUuid(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                new UuidStatementValue(uuid),
                new IntegerStatementValue(amount)
                )
        );

        try {
            while(result.next()) {
                logList.add(result.getTimestamp(1).toString() + " " + result.getString(2));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return logList;
    }


    public static int logID(String message, Object... args) {
        assert database != null;

        String logMessage = String.format(message, args);
        log.info(logMessage);
        StringStatementValue stringStatementValueLevel = new StringStatementValue("LEGEND");
        StringStatementValue stringStatementValueMessage = new StringStatementValue(logMessage);
        AtomicInteger id = new AtomicInteger(-1);
        database.executeUpdate(new Statement("INSERT INTO logs (Level, Message) VALUES (?, ?)",
                stringStatementValueLevel,
                stringStatementValueMessage
        ), resultSet -> {
            try {
                if (resultSet.next()) {
                    id.set(resultSet.getInt(1));
                }
            } catch (SQLException ex) {
              ex.printStackTrace();
            }
        });
        return id.get();
    }

     public enum UuidLogType {
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

    }
}
