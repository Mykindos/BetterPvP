package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.formattedtypes.JoinClanLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ClanLogger {

    private static Database database;
    private static ClanManager clanManager;

    @Inject
    public ClanLogger(Database database, ClanManager clanManager, ClientManager clientManager) {
        ClanLogger.database = database;
        ClanLogger.clanManager = clanManager;
    }

    /**
     * Add the logs related to the specified clan log
     * @param clanLog the specified clan log
     */
    public static void addClanLog(ClanLog clanLog) {
        database.executeBatch(clanLog.getStatements(), true);
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

    public static FormattedClanLog formattedLogFromRow(long time, UUID player1ID, UUID clan1ID, UUID player2ID, UUID clan2ID, ClanLogType type) {
        OfflinePlayer offlinePlayer1 = null;
        if (player1ID != null) {
            offlinePlayer1 = Bukkit.getOfflinePlayer(player1ID);
        }
        Clan clan1 = null;
        if (clan1ID != null) {
            clan1 = clanManager.getClanById(clan1ID).orElse(null);
        }
        OfflinePlayer offlinePlayer2 = null;
        if (player2ID != null) {
            offlinePlayer2 = Bukkit.getOfflinePlayer(player2ID);
        }
        Clan clan2 = null;
        if (clan2ID != null) {
            clan2 = clanManager.getClanById(clan2ID).orElse(null);
        }

        switch (type) {
            case JOIN -> {
                return new JoinClanLog(time, offlinePlayer1, clan2);
            }
            default -> {
                return new FormattedClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2, type);
            }
        }

    }
}