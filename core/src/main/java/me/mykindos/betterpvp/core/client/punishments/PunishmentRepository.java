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
import me.mykindos.betterpvp.core.database.jooq.tables.records.PunishmentsRecord;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.PUNISHMENTS;

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
        UtilServer.runTaskAsync(core, () -> {
            try {
                DSLContext ctx = database.getDslContext();
                PunishmentsRecord punishmentRecord = ctx.newRecord(PUNISHMENTS);
                punishmentRecord.setId(punishment.getId());
                punishmentRecord.setClient(punishment.getClientId());
                punishmentRecord.setType(punishment.getType().getName());
                punishmentRecord.setRule(punishment.getRule().getKey().toLowerCase().replace(' ', '_'));
                punishmentRecord.setApplyTime(punishment.getApplyTime());
                punishmentRecord.setExpiryTime(punishment.getExpiryTime());
                punishmentRecord.setReason(punishment.getReason());
                punishmentRecord.setPunisher(punishment.getPunisher() == null ? "" : punishment.getPunisher().toString());

                punishmentRecord.insert();
                log.info("Saved punishment {} to database", punishment).submit();
            } catch (Exception ex) {
                log.error("Error saving punishment {}", punishment, ex).submit();
            }
        });
    }

    public void revokePunishment(Punishment punishment) {
        UtilServer.runTaskAsync(core, () -> {
            database.getDslContext()
                    .update(PUNISHMENTS)
                    .set(PUNISHMENTS.REVOKER, Objects.requireNonNull(punishment.getRevoker()).toString())
                    .set(PUNISHMENTS.REVOKE_TYPE, punishment.getRevokeType() != null ? punishment.getRevokeType().name() : null)
                    .set(PUNISHMENTS.REVOKE_TIME, punishment.getRevokeTime())
                    .set(PUNISHMENTS.REVOKE_REASON, punishment.getRevokeReason())
                    .where(PUNISHMENTS.ID.eq(punishment.getId()))
                    .execute();

            log.info("Marked punishment as revoked in database - {}", punishment).submit();
        });
    }

    public List<Punishment> getPunishmentsForClient(Client client) {
        List<Punishment> punishments = new ArrayList<>();

        try {
            var records = database.getDslContext()
                    .select(PUNISHMENTS.asterisk())
                    .from(PUNISHMENTS)
                    .join(CLIENTS).on(PUNISHMENTS.CLIENT.eq(CLIENTS.ID))
                    .where(PUNISHMENTS.CLIENT.eq(client.getId()))
                    .fetch();

            for (var punishmentRecord : records) {
                int punishmentId = punishmentRecord.get(PUNISHMENTS.ID);
                long punishedClientId = punishmentRecord.get(CLIENTS.ID);
                UUID punishedClientUUID = UUID.fromString(punishmentRecord.get(CLIENTS.UUID));
                IPunishmentType type = PunishmentTypes.getPunishmentType(punishmentRecord.get(PUNISHMENTS.TYPE));
                Rule rule = ruleManager.getOrCustom(punishmentRecord.get(PUNISHMENTS.RULE).toLowerCase().replace('_', ' '));
                long applyTime = punishmentRecord.get(PUNISHMENTS.APPLY_TIME);
                long expiryTime = punishmentRecord.get(PUNISHMENTS.EXPIRY_TIME);
                String reason = punishmentRecord.get(PUNISHMENTS.REASON);
                UUID punisher = UUID.fromString(punishmentRecord.get(PUNISHMENTS.PUNISHER));
                UUID revoker = UUID.fromString(punishmentRecord.get(PUNISHMENTS.REVOKER));
                String revokeTypeString = punishmentRecord.get(PUNISHMENTS.REVOKE_TYPE);
                RevokeType revokeType = revokeTypeString == null ? null : RevokeType.valueOf(revokeTypeString);
                long revokeTime = punishmentRecord.get(PUNISHMENTS.REVOKE_TIME);
                String revokeReason = punishmentRecord.get(PUNISHMENTS.REVOKE_REASON);

                Punishment punishment = new Punishment(
                        punishmentId,
                        punishedClientId,
                        punishedClientUUID,
                        type,
                        rule,
                        applyTime,
                        expiryTime,
                        reason,
                        punisher,
                        revoker,
                        revokeType,
                        revokeTime,
                        revokeReason);

                punishments.add(punishment);
            }
        } catch (Exception ex) {
            log.error("Error loading punishments for client {}", client.getUniqueId(), ex).submit();
        }
        return punishments;
    }

}
