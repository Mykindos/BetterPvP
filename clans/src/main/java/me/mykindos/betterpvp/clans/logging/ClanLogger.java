package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import lombok.CustomLog;
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

@CustomLog
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
        PLAYER,
        CLAN,
        NONE
    }

    public enum ClanLogType {
        /**
         * The player joins a Clan
         */
        JOIN,
        /**
         * The player leaves a Clan
         */
        LEAVE,
        /**
         * The player kicks another player from a Clan
         */
        KICKER,
        /**
         * The player is kicked from a Clan
         */
        KICKED,
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
