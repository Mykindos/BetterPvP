package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.OldClanManager;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;
import me.mykindos.betterpvp.clans.logging.types.log.ClanLog;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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
    private final ClanLogHolderManager clanLogHolderManager;

    @Inject
    public ClanLogger(Database database, ClanManager clanManager, OldClanManager oldClanManager, ClanLogHolderManager clanLogHolderManager, ClanLogHolderManager clanLogHolderManager1) {
        this.database = database;
        this.clanManager = clanManager;
        this.oldClanManager = oldClanManager;
        this.clanLogHolderManager = clanLogHolderManager1;
    }

    /**
     * Add the logs related to the specified clan log
     * @param clanLog the specified clan log
     */
    public void addClanLog(ClanLog clanLog) {
        clanLogHolderManager.addClanLogs(clanLog);
    }

    public void addClanKill(UUID killID, Player killer, Player victim) {
            Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);
            Clan victimClan = clanManager.getClanByPlayer(victim).orElse(null);
            double dominance = 0;
            if (clanManager.getRelation(killerClan, victimClan).equals(ClanRelation.ENEMY)) {
                assert killerClan != null;
                assert victimClan != null;
                dominance = clanManager.getDominanceForKill(killerClan.getSquadCount(), victimClan.getSquadCount());
            }
            clanLogHolderManager.addClanKill(killID, killerClan, victimClan, dominance);
    }

    public List<FormattedClanLog> getAllLogs(Clan clan) {
        return clanLogHolderManager.getClanLogs(clan);
    }

    public List<KillClanLog> getClanKillLogs(Clan clan) {
        return clanLogHolderManager.getClanKillLogs(clan);
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
                String uuid = result.getString(1);
                return java.util.UUID.fromString(uuid);
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
}
