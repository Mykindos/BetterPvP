package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.OldClanManager;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.JoinClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;
import me.mykindos.betterpvp.clans.logging.types.log.ClanLog;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
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
    private final OldClanManager oldClanManager;

    @Inject
    public ClanLogger(Database database, ClanManager clanManager, OldClanManager oldClanManager) {
        this.database = database;
        this.clanManager = clanManager;
        this.oldClanManager = oldClanManager;
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

    public void addClanKill(UUID KillID, Player killer, Player victim) {
        log.info("1");
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            log.info("here");
            String query = "INSERT INTO clans_kills (KillId, KillerClan, VictimClan, Dominance) VALUES (?, ?, ?, ?)";
            Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);
            Clan victimClan = clanManager.getClanByPlayer(victim).orElse(null);
            double dominance = 0;
            if (clanManager.getRelation(killerClan, victimClan).equals(ClanRelation.ENEMY)) {
                assert killerClan != null;
                assert victimClan != null;
                dominance = clanManager.getDominanceForKill(killerClan.getSquadCount(), victimClan.getSquadCount());
            }

            database.executeUpdate(new Statement(query,
                    new UuidStatementValue(KillID),
                    new StringStatementValue(killerClan == null ? null : String.valueOf(killerClan.getId())),
                    new StringStatementValue(victimClan == null ? null : String.valueOf(victimClan.getId())),
                    new DoubleStatementValue(dominance)
                    ));
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

    public List<KillClanLog> getClanKillLogs(Clan clan, int amount) {
        List<KillClanLog> logList = new ArrayList<>();

        if (amount < 0) {
            return logList;
        }

        String query = "CALL GetClanKillLogsByClan(?, ?)";
        CachedRowSet result = database.executeQuery(new Statement(query,
                        new UuidStatementValue(clan.getId()),
                        new IntegerStatementValue(amount)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                String killerID = result.getString(2);
                String killerClanID = result.getString(3);
                String victimID = result.getString(4);
                String victimClanID = result.getString(5);
                double dominance = result.getDouble(6);

                OfflinePlayer killer =  Bukkit.getOfflinePlayer(UUID.fromString(killerID));;
                IOldClan killerClan = oldClanManager.getOldClan(killerClanID);

                OfflinePlayer victim = Bukkit.getOfflinePlayer(UUID.fromString(victimID));;
                IOldClan victimClan = oldClanManager.getOldClan(victimClanID);

                logList.add(new KillClanLog(clan, time, killer, killerClan, victim, victimClan, dominance));
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

    public List<OfflinePlayer> getPlayersByClan(UUID clanID) {
        List<OfflinePlayer> offlinePlayers = new ArrayList<>();
        String query = "Call GetPlayersByClan(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                new UuidStatementValue(clanID)
                )
        );

        try {
            while (result.next()) {
                String playerID = result.getString(1);

                offlinePlayers.add(Bukkit.getOfflinePlayer(UUID.fromString(playerID)));
            }
        } catch (SQLException ex) {
            log.error("Failed to get playerID from Clan logs", ex);
        }

        return offlinePlayers;
    }

    public List<IOldClan> getClansByPlayer(UUID playerID) {
        List<IOldClan> clans = new ArrayList<>();
        String query = "Call GetClansByPlayer(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(playerID)
                )
        );

        try {
            while (result.next()) {
                String clanID = result.getString(1);
                clans.add(oldClanManager.getOldClan(clanID));
            }
        } catch (SQLException ex) {
            log.error("Failed to get clanIDs from Clan logs", ex);
        }
        return clans;
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
            default -> {
                return new FormattedClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2, type);
            }
        }

    }
}
