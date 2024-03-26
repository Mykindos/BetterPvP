package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ClanLogger {

    private static Database database;

    @Inject
    public ClanLogger(Database database) {
        ClanLogger.database = database;
    }

    /**
     * Add a reference to the specified log id for a clan log
     * @param logUUID the ID of the log this information is about
     * @param uuid the UUID to reference
     * @param uuidType the type of UUID this is
     * @param type the type of log this is
     */
    public static void addClanLogMeta(UUID logUUID, @Nullable UUID uuid, ClanLogger.UUIDType uuidType, ClanLogger.ClanLogType type) {
        String query = "INSERT INTO clanlogmeta (LogUUID, UUID, UUIDType, type) VALUES (?, ?, ?, ?)";
        database.executeUpdate(new Statement(query,
                        new UuidStatementValue(logUUID),
                        new StringStatementValue(uuid == null ? null : uuid.toString()),
                        new StringStatementValue(uuidType.name()),
                        new StringStatementValue(type.name())
                )
        );
    }

    public static List<String> getAllLogs() {
        List<String> logList = new ArrayList<>();
        String query = "SELECT * FROM clanlogs";
        CachedRowSet result = database.executeQuery( new Statement(query)
        );

        try {
            while (result.next()) {
                String id = result.getString(1);
                long time = result.getLong(2);
                String type = result.getString(3);
                String Player1 = result.getString(4);
                String Clan1 = result.getString(5);
                String Player2 = result.getString(6);
                String Clan2 = result.getString(7);

                logList.add(String.format("ID %s time %s type %s Player1 %s Clan1 %s Player2 %s Clan2 %s", id, time, type, Player1, Clan1, Player2, Clan2));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    public static List<String> getClanLogs(UUID clanUUID, int amount) {
        List<String> logList = new ArrayList<>();

        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetClanLogsByClanUuidAmount(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(clanUUID),
                        new IntegerStatementValue(amount)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                logList.add("<green>" + UtilTime.getTime((System.currentTimeMillis() - time), 2) + " ago</green> " + result.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    public static List<String> getClanKillLogs(UUID clanUUID, int amount) {
        List<String> logList = new ArrayList<>();

        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetWPLogs(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(clanUUID),
                        new IntegerStatementValue(amount)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                logList.add("<green>" + UtilTime.getTime((System.currentTimeMillis() - time), 2) + " ago</green> " + result.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    public static List<String> getJoinLeaveMessages(UUID clanUUID, int amount) {
        List<String> logList = new ArrayList<>();

        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetClanJoinLeaveMessagesByClanUUID(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(clanUUID),
                        new IntegerStatementValue(amount)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                logList.add("<green>" + UtilTime.getTime((System.currentTimeMillis() - time), 2) + " ago</green> " + result.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanJoinLeave messages", ex);
        }
        return logList;
    }

    public enum UUIDType {
        /**
         * Represents the primary player, generally the player doing the action
         */
        PLAYER1,
        /**
         * Represents a secondary player, general the player the action occurs to
         */
        PLAYER2,
        /**
         * The clan of PLAYER1
         */
        CLAN1,
        /**
         * The clan of PLAYER2
         */
        CLAN2,
        /**
         * The UUID is intentionally left null, and does not represent anything
         */
        NONE
    }

    public enum ClanLogType {
        /**
         * A player joins a Clan
         */
        JOIN,
        /**
         * A player leaves a Clan
         */
        LEAVE,
        /**
         * A player is kicked from a Clan
         */
        KICK,
        /**
         *
         */
        CREATE,
        /**
         *
         */
        DISBAND
        //TODO more enums

    }
}
