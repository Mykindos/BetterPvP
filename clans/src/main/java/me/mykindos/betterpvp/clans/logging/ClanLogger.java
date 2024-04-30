package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
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
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilUUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
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

    public void addClanKill(UUID killID, Player killer, Player victim) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);
            Clan victimClan = clanManager.getClanByPlayer(victim).orElse(null);
            double dominance = 0;
            if (clanManager.getRelation(killerClan, victimClan).equals(ClanRelation.ENEMY)) {
                assert killerClan != null;
                assert victimClan != null;
                if (!killerClan.isNoDominanceCooldownActive() && !victimClan.isNoDominanceCooldownActive()) {
                    dominance = clanManager.getDominanceForKill(killerClan.getSquadCount(), victimClan.getSquadCount());
                }
            }
            
            String query = "INSERT INTO clans_kills (KillId, KillerClan, VictimClan, Dominance) VALUES (?, ?, ?, ?)";
            database.executeUpdate(new Statement(query,
                    new UuidStatementValue(killID),
                    new StringStatementValue(killerClan == null ? null : String.valueOf(killerClan.getId())),
                    new StringStatementValue(victimClan == null ? null : String.valueOf(victimClan.getId())),
                    new DoubleStatementValue(dominance)
            ));
        });
    }

    public List<FormattedClanLog> getClanLogs(UUID clanID) {
        List<FormattedClanLog> logList = new ArrayList<>();
        String query = "Call GetClanLogsByClan(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(clanID)
                )
        );

        try {
            while (result.next()) {
                long time = result.getLong(1);
                String type = result.getString(2);
                String mainPlayerName = result.getString(3);
                String mainClan = result.getString(4);
                String mainClanName = result.getString(5);
                String otherPlayerName = result.getString(6);
                String otherClan = result.getString(7);
                String otherClanName = result.getString(8);

                logList.add(formattedLogFromRow(time,
                        mainPlayerName,
                        mainClan,
                        mainClanName,
                        otherPlayerName,
                        otherClan,
                        otherClanName,
                        ClanLogType.valueOf(type)));
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
                String killerName = result.getString(2);
                String killerClanID = result.getString(3);
                String killerClanName = result.getString(4);
                String victimName = result.getString(5);
                String victimClanID = result.getString(6);
                String victimClanName = result.getString(7);
                double dominance = result.getDouble(8);

                UUID killerClan = UtilUUID.fromString(killerClanID);

                UUID victimClan = UtilUUID.fromString(victimClanID);

                logList.add(new KillClanLog(clan, time, killerName, killerClan, killerClanName, victimName, victimClan, victimClanName, dominance));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return logList;
    }

    public Component getClanUUIDOfPlayerAtTime(UUID playerID, long time) {
        String query = "CALL GetClanByPlayerAtTime(?, ?)";
        CachedRowSet result = database.executeQuery(new Statement(query,
                        new UuidStatementValue(playerID),
                        new LongStatementValue(time)
                )
        );
        try {
            while (result.next()) {
                String uuid = result.getString(1);
                String name = result.getString(2);
                return (Component.text(name).hoverEvent(HoverEvent.showText(Component.text(uuid))));
            }
        } catch (SQLException ex) {
            log.error("Failed to get ClanUUID logs", ex);
        }
        return null;
    }

    public List<String> getPlayersByClan(UUID clanID) {
        List<String> playerNames = new ArrayList<>();
        String query = "Call GetPlayersByClan(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                new UuidStatementValue(clanID)
                )
        );

        try {
            while (result.next()) {
                String playerName = result.getString(1);

                playerNames.add(playerName);
            }
        } catch (SQLException ex) {
            log.error("Failed to get playerID from Clan logs", ex);
        }

        return playerNames;
    }

    public List<Component> getClansByPlayer(UUID playerID) {
        List<Component> clans = new ArrayList<>();
        String query = "Call GetClansByPlayer(?)";
        CachedRowSet result = database.executeQuery( new Statement(query,
                        new UuidStatementValue(playerID)
                )
        );

        try {
            while (result.next()) {
                String clanID = result.getString(1);
                String clanName = result.getString(2);
                clans.add(Component.text(clanName).hoverEvent(HoverEvent.showText(Component.text(clanID))));
            }
        } catch (SQLException ex) {
            log.error("Failed to get clanIDs from Clan logs", ex);
        }
        return clans;
    }
    public FormattedClanLog formattedLogFromRow(long time,
                                                String mainPlayerName,
                                                String mainClanID,
                                                String mainClanName,
                                                String otherPlayerName,
                                                String otherClanID,
                                                String otherClanName,
                                                ClanLogType type) {

        UUID mainClan = UtilUUID.fromString(mainClanID);

        UUID otherClan = UtilUUID.fromString(otherClanID);

        switch (type) {
            case CLAN_JOIN -> {
                return new JoinClanLog(time, mainPlayerName, mainClan, mainClanName);
            }
            case CLAN_KICK -> {
                return new KickClanLog(time, mainPlayerName, mainClan, mainClanName, otherPlayerName);
            }
            case CLAN_CLAIM -> {
                return new ClaimClanLog(time, mainPlayerName, mainClan, mainClanName);
            }
            case CLAN_ENEMY -> {
                return new EnemyClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_LEAVE -> {
                return new LeaveClanLog(time, mainPlayerName, mainClan, mainClanName);
            }
            case CLAN_CREATE -> {
                return new CreateClanLog(time, mainPlayerName, mainClan, mainClanName);
            }
            case CLAN_DEMOTE -> {
                return new DemoteClanLog(time, mainPlayerName, mainClan, mainClanName, otherPlayerName, otherClan, otherClanName);
            }
            case CLAN_INVITE -> {
                return new InviteClanLog(time, mainPlayerName, mainClan, mainClanName, otherPlayerName);
            }
            case CLAN_PROMOTE -> {
                return new PromoteClanLog(time, mainPlayerName, mainClan, mainClanName, otherPlayerName, otherClan, otherClanName);
            }
            case CLAN_SETHOME -> {
                return new SetHomeClanLog(time, mainPlayerName, mainClan, mainClanName);
            }
            case CLAN_UNCLAIM -> {
                return new UnclaimClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_TRUST_ACCEPT -> {
                return new TrustAcceptLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_ALLIANCE_ACCEPT -> {
                return new AllianceAcceptClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_TRUST_REMOVE -> {
                return new TrustRemoveLog(time, mainPlayerName, mainClan, mainClanName, otherClan, mainClanName);
            }
            case CLAN_TRUST_REQUEST -> {
                return new TrustRequestClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_NEUTRAL_ACCEPT -> {
                return new NeutralAcceptClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_ALLIANCE_REMOVE -> {
                return new AllianceRemoveClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_NEUTRAL_REQUEST -> {
                return new NeutralRequestClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            case CLAN_ALLIANCE_REQUEST -> {
                return new AllianceRequestClanLog(time, mainPlayerName, mainClan, mainClanName, otherClan, otherClanName);
            }
            default -> {
                return new FormattedClanLog(time, mainPlayerName, mainClan, mainClanName, otherPlayerName, otherClan, otherClanName, type);
            }
        }
    }
}
