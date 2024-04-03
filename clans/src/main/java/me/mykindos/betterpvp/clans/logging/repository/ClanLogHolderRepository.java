package me.mykindos.betterpvp.clans.logging.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.OldClanManager;
import me.mykindos.betterpvp.clans.logging.types.ClanLogHolder;
import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.clans.logging.types.formatted.AllianceAcceptClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.AllianceRemoveClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.AllianceRequestClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.ClaimClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.CreateClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.DemoteClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.EnemyClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.InviteClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.JoinClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KickClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.LeaveClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.NeutralAcceptClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.NeutralRequestClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.PromoteClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.SetHomeClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.TrustAcceptLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.TrustRemoveLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.TrustRequestClanLog;
import me.mykindos.betterpvp.clans.logging.types.formatted.UnclaimClanLog;
import me.mykindos.betterpvp.clans.logging.types.log.ClanLog;
import me.mykindos.betterpvp.core.components.clans.IOldClan;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.apache.commons.lang.NotImplementedException;
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
public class ClanLogHolderRepository implements IRepository<ClanLogHolder> {
    private final Database database;
    private final OldClanManager oldClanManager;

    @Inject
    public ClanLogHolderRepository(Database database, OldClanManager oldClanManager) {
        this.database = database;
        this.oldClanManager = oldClanManager;
    }

    @Override
    public List<ClanLogHolder> getAll() {
        throw new NotImplementedException();
    }

    public void addClanLog(ClanLog clanLog) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            database.executeUpdate(clanLog.getLogTimeStatetment());
            database.executeBatch(clanLog.getStatements(), true);
        });
    }

    public void addClanKill(UUID KillID, Clan killerClan, Clan victimClan, double dominance) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            String query = "INSERT INTO clans_kills (KillId, KillerClan, VictimClan, Dominance) VALUES (?, ?, ?, ?)";
            database.executeUpdate(new Statement(query,
                    new UuidStatementValue(KillID),
                    new StringStatementValue(killerClan == null ? null : String.valueOf(killerClan.getId())),
                    new StringStatementValue(victimClan == null ? null : String.valueOf(victimClan.getId())),
                    new DoubleStatementValue(dominance)
            ));
        });
    }

    public ClanLogHolder get(Clan clan) {
        ClanLogHolder clanLogHolder = new ClanLogHolder(clan.getId());
        clanLogHolder.getClanLogs().addAll(getClanLogs(clan.getId()));
        clanLogHolder.setClanLogsUpdated(true);
        clanLogHolder.getClanKillLogs().addAll(getClanKillLogs(clan));
        clanLogHolder.setKillLogsUpdated(true);
        return clanLogHolder;
    }

    public List<FormattedClanLog> getClanLogs(UUID ClanID) {
        List<FormattedClanLog> logList = new ArrayList<>();
        String query = "Call GetClanLogsByClan(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                new UuidStatementValue(ClanID)
                )
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

    public List<KillClanLog> getClanKillLogs(Clan clan) {
        List<KillClanLog> logList = new ArrayList<>();

        String query = "CALL GetClanKillLogsByClan(?)";
        CachedRowSet result = database.executeQuery(new Statement(query,
                        new UuidStatementValue(clan.getId())
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

    @Override
    public void save(ClanLogHolder object) {
        IRepository.super.save(object);
    }

    public FormattedClanLog formattedLogFromRow(long time, String player1ID, String clan1ID, String player2ID, String clan2ID, ClanLogType type) {
        OfflinePlayer offlinePlayer1 = null;
        if (player1ID != null) {
            offlinePlayer1 = Bukkit.getOfflinePlayer(UUID.fromString(player1ID));
        }

        IOldClan clan1 = oldClanManager.getOldClan(clan1ID);

        OfflinePlayer offlinePlayer2 = null;
        if (player2ID != null) {
            offlinePlayer2 = Bukkit.getOfflinePlayer(UUID.fromString(player2ID));
        }

        IOldClan clan2 = oldClanManager.getOldClan(clan2ID);

        switch (type) {
            case CLAN_JOIN -> {
                return new JoinClanLog(time, offlinePlayer1, clan1);
            }
            case CLAN_KICK -> {
                return new KickClanLog(time, offlinePlayer1, clan1, offlinePlayer2);
            }
            case CLAN_CLAIM -> {
                return new ClaimClanLog(time, offlinePlayer1, clan1);
            }
            case CLAN_ENEMY -> {
                return new EnemyClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_LEAVE -> {
                return new LeaveClanLog(time, offlinePlayer1, clan1);
            }
            case CLAN_CREATE -> {
                return new CreateClanLog(time, offlinePlayer1, clan1);
            }
            case CLAN_DEMOTE -> {
                return new DemoteClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2);
            }
            case CLAN_INVITE -> {
                return new InviteClanLog(time, offlinePlayer1, clan1, offlinePlayer2);
            }
            case CLAN_PROMOTE -> {
                return new PromoteClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2);
            }
            case CLAN_SETHOME -> {
                return new SetHomeClanLog(time, offlinePlayer1, clan1);
            }
            case CLAN_UNCLAIM -> {
                return new UnclaimClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_TRUST_ACCEPT -> {
                return new TrustAcceptLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_ALLIANCE_ACCEPT -> {
                return new AllianceAcceptClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_TRUST_REMOVE -> {
                return new TrustRemoveLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_TRUST_REQUEST -> {
                return new TrustRequestClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_NEUTRAL_ACCEPT -> {
                return new NeutralAcceptClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_ALLIANCE_REMOVE -> {
                return new AllianceRemoveClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_NEUTRAL_REQUEST -> {
                return new NeutralRequestClanLog(time, offlinePlayer1, clan1, clan2);
            }
            case CLAN_ALLIANCE_REQUEST -> {
                return new AllianceRequestClanLog(time, offlinePlayer1, clan1, clan2);
            }
            default -> {
                return new FormattedClanLog(time, offlinePlayer1, clan1, offlinePlayer2, clan2, type);
            }
        }
    }
}
