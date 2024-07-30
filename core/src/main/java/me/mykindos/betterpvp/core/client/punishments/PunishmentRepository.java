package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
public class PunishmentRepository implements IRepository<Punishment> {

    private final Core core;
    private final Database database;

    @Inject
    public PunishmentRepository(Core core, Database database) {
        this.core = core;
        this.database = database;
    }

    @Override
    public void save(Punishment punishment) {
        String query = "INSERT INTO punishments (id, Client, Type, ExpiryTime, Reason, Punisher) VALUES (?, ?, ?, ?, ?, ?)";
        UtilServer.runTaskAsync(core, () -> {
            Statement statement = new Statement(query,
                    new UuidStatementValue(punishment.getId()),
                    new UuidStatementValue(punishment.getClient()),
                    new StringStatementValue(punishment.getType().getName()),
                    new LongStatementValue(punishment.getExpiryTime()),
                    new StringStatementValue(punishment.getReason()),
                    new StringStatementValue(punishment.getPunisher()));

            database.executeUpdate(statement, TargetDatabase.GLOBAL);
            log.info("Saved punishment {} to database", punishment).submit();
        });
    }

    public void revokePunishment(Punishment punishment) {
        String query = "UPDATE punishments SET Revoked = 1, TimeRevoked = ? WHERE id = ?";
        UtilServer.runTaskAsync(core, () -> {
            Statement statement = new Statement(query, new TimestampStatementValue(new Timestamp(System.currentTimeMillis())), new UuidStatementValue(punishment.getId()));
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
                Punishment punishment = new Punishment(
                        UUID.fromString(result.getString(1)),
                        UUID.fromString(result.getString(2)),
                        PunishmentTypes.getPunishmentType(result.getString(3)),
                        result.getLong(4),
                        result.getString(5),
                        result.getString(6),
                        result.getBoolean(8)
                );

                punishments.add(punishment);
            }
        } catch (SQLException e) {
            log.error("Error while retrieving punishments for client {}", client.getUniqueId(), e).submit();
        }
        return punishments;
    }

}
