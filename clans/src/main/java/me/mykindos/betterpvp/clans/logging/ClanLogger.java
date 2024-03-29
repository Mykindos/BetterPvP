package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.JoinClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;
import me.mykindos.betterpvp.clans.logging.types.log.ClanLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CustomLog
@Singleton
public class ClanLogger {

    private final Database database;
    private final ClanManager clanManager;

    @Inject
    public ClanLogger(Database database, ClanManager clanManager) {
        this.database = database;
        this.clanManager = clanManager;
    }

    /**
     * Add the logs related to the specified clan log
     * @param clanLog the specified clan log
     */
    public void addClanLog(ClanLog clanLog) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            database.executeUpdate(clanLog.getLogTimeStatetment());
            database.executeBatch(clanLog.getStatements(), true);
        });

    }

    public List<FormattedClanLog> getAllLogs(UUID ClanID, int amount) {
        List<FormattedClanLog> logList = new ArrayList<>();
        String query = "Call GetClanLogsByClan(?, ?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(ClanID),
                        new IntegerStatementValue(amount))
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                String type = result.getString(2);
                String Player1 = result.getString(3);
                String Clan1 = result.getString(4);
                String Player2 = result.getString(5);
                String Clan2 = result.getString(6);

                logList.add(formattedLogFromRow(time, Player1, Clan1, Player2, Clan2, ClanLogType.valueOf(type)));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    public List<FormattedClanLog> getClanKillLogs(UUID clanUUID, int amount) {
        List<FormattedClanLog> logList = new ArrayList<>();

        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetClanKillLogs(?, ?)";
        CachedRowSet result = database.executeQuery(new Statement(query,
                        new UuidStatementValue(clanUUID),
                        new IntegerStatementValue(amount)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                String killerID = result.getString(2);
                String killerClanID = result.getString(3);
                String victimID = result.getString(4);
                String victimClanID = result.getString(6);
                logList.add(formattedLogFromRow(time, killerID, killerClanID, victimID, victimClanID, ClanLogType.CLAN_KILL));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    public UUID getClanUUIDOfPlayerAtTime(UUID playerID, long time) {
        String query = "CALL GetClanByPlayerAtTime(?, ?)";
        CachedRowSet result = database.executeQuery(new Statement(query,
                        new UuidStatementValue(playerID),
                        new LongStatementValue(time)
                )
        );
        try {
            while (result.next()) {
                String UUID = result.getString(1);
                return java.util.UUID.fromString(UUID);
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return null;
    }

    public FormattedClanLog formattedLogFromRow(long time, String player1ID, String clan1ID, String player2ID, String clan2ID, ClanLogType type) {
        OfflinePlayer offlinePlayer1 = null;
        if (player1ID != null) {
            offlinePlayer1 = Bukkit.getOfflinePlayer(UUID.fromString(player1ID));
        }
        Clan clan1 = null;
        if (clan1ID != null) {
            clan1 = clanManager.getClanById(UUID.fromString(clan1ID)).orElse(null);
        }
        OfflinePlayer offlinePlayer2 = null;
        if (player2ID != null) {
            offlinePlayer2 = Bukkit.getOfflinePlayer(UUID.fromString(player2ID));
        }
        Clan clan2 = null;
        if (clan2ID != null) {
            clan2 = clanManager.getClanById(UUID.fromString(clan2ID)).orElse(null);
        }

        switch (type) {
            case CLAN_JOIN -> {
                return new JoinClanLog(time, offlinePlayer1, clan1);
            }
            case CLAN_KILL -> {
                return new KillClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2);
            }
            default -> {
                return new FormattedClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2, type);
            }
        }

    }
}
