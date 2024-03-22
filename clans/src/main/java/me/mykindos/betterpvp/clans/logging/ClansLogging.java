package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.logging.data.logdata.ChangeClanLogData;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilRegex;
import me.mykindos.betterpvp.core.utilities.UtilTime;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Singleton
@Slf4j
public class ClansLogging {
    private static Database database;

    private static final String joinRegex =
            "^" + UtilRegex.PLAYERNAME + " \\(" + UtilRegex.UUID + "\\) joined " + UtilRegex.Clanname(3, 13) + "\\(" + UtilRegex.UUID + "\\)$";
    private static final Pattern leftRegex = Pattern.compile("\\) left .{3,}\\(.{36}\\)$");

    @Inject
    public ClansLogging(Database database) {
        ClansLogging.database = database;
    }

    /**
     *
     * @param clanUUID the uuid of the item
     * @param amount the number of logs to retrieve
     * @return A list of the last amount of logs relating to this uiid
     */
    public static List<String> getClanUuidLogs(UUID clanUUID, int amount) {
        log.info(joinRegex);
        List<String> logList = new ArrayList<>();
        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetClanLogsByClanUUID(?, ?)";
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

    public static List<ChangeClanLogData> getChangeClanLogDataByClanUUID(UUID clanUUID) {
        List<ChangeClanLogData> logList = new ArrayList<>();

        String query = "CALL GetClanJoinLeaveLogsByClanUUID(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(clanUUID)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                String message = result.getString(2);
                logList.add(parseChangeClanLog(time, message));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    private static ChangeClanLogData parseChangeClanLog(long time, String message) {
        ChangeClanLogData.ChangeClanLogType type = null;
        if (message.contains("joined")) {

        }
        return null;
    }


}
