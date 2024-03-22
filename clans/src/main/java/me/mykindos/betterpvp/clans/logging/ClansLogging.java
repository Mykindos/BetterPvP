package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilTime;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@Slf4j
public class ClansLogging {
    private static Database database;

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
}
