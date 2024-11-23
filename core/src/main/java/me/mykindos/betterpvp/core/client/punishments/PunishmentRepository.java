package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
public class PunishmentRepository implements IRepository<Punishment> {

    private final Core core;
    private final Database database;
    private final RuleManager ruleManager;

    @Inject
    public PunishmentRepository(Core core, Database database, RuleManager ruleManager) {
        this.core = core;
        this.database = database;
        this.ruleManager = ruleManager;
    }

    @Override
    public void save(Punishment punishment) {
        String query = "INSERT INTO punishments (id, Client, Type, Rule, ApplyTime, ExpiryTime, Reason, Punisher) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        UtilServer.runTaskAsync(core, () -> {
            Statement statement = new Statement(query,
                    new UuidStatementValue(punishment.getId()),
                    new UuidStatementValue(punishment.getClient()),
                    new StringStatementValue(punishment.getType().getName()),
                    new StringStatementValue(punishment.getRule().getKey()),
                    new LongStatementValue(punishment.getApplyTime()),
                    new LongStatementValue(punishment.getExpiryTime()),
                    new StringStatementValue(punishment.getReason()),
                    new UuidStatementValue(punishment.getPunisher()));

            database.executeUpdate(statement, TargetDatabase.GLOBAL);
            log.info("Saved punishment {} to database", punishment).submit();
        });
    }

    public void revokePunishment(Punishment punishment) {
        String query = "UPDATE punishments SET Revoker = ?, RevokeType = ?, RevokeTime = ?, RevokeReason = ? WHERE id = ?";
        UtilServer.runTaskAsync(core, () -> {
            Statement statement = new Statement(query,
                    new UuidStatementValue(punishment.getRevoker()),
                    new StringStatementValue(punishment.getRevokeType() != null ? punishment.getRevokeType().name() : null),
                    new LongStatementValue(punishment.getRevokeTime()),
                    new StringStatementValue(punishment.getRevokeReason()),
                    new UuidStatementValue(punishment.getId()));
            database.executeUpdate(statement, TargetDatabase.GLOBAL);
            log.info("Marked punishment as revoked in database - {}", punishment).submit();
        });
    }

    public List<Punishment> getPunishmentsForClient(Client client) {
        List<Punishment> punishments = new ArrayList<>();

        String query = "SELECT * FROM punishments WHERE Client = ?";
        Statement statement = new Statement(query, new UuidStatementValue(client.getUniqueId()));

        CachedRowSet result = database.executeQuery(statement, TargetDatabase.GLOBAL);
        try {
            while (result.next()) {
                UUID id = UUID.fromString(result.getString(1));
                UUID punishedClient = UUID.fromString(result.getString(2));
                IPunishmentType type = PunishmentTypes.getPunishmentType(result.getString(3));
                Rule rule = ruleManager.getOrCustom(result.getString(4));
                long applyTime = result.getLong(5);
                long expiryTime = result.getLong(6);
                String reason = result.getString(7);
                String punisherString = result.getString(8);
                UUID punisher = punisherString == null ? null : UUID.fromString(punisherString);
                String revokerString = result.getString(9);
                UUID revoker = revokerString == null ? null : UUID.fromString(revokerString);
                String revokeTypeString = result.getString(10);
                RevokeType revokeType = revokeTypeString == null ? null : RevokeType.valueOf(revokeTypeString);
                long revokeTime = result.getLong(11);
                String revokeReason = result.getString(12);

                Punishment punishment = new Punishment(
                        id,
                        punishedClient,
                        type,
                        rule,
                        applyTime,
                        expiryTime,
                        reason,
                        punisher,
                        revoker,
                        revokeType,
                        revokeTime,
                        revokeReason
                );

                punishments.add(punishment);
            }
        } catch (SQLException e) {
            log.error("Error while retrieving punishments for client {}", client.getUniqueId(), e).submit();
        }
        return punishments;
    }

}
