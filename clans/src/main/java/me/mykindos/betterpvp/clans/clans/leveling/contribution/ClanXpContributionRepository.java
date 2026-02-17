package me.mykindos.betterpvp.clans.clans.leveling.contribution;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * Persists per-member XP contribution data to the {@code clan_xp_contributions} DB table.
 *
 * <p>Writes are done asynchronously to avoid blocking the main thread on each XP gain event.
 * Reads are synchronous and only happen at server startup during clan loading.
 */
@Singleton
@CustomLog
public class ClanXpContributionRepository {

    private final Database database;

    @Inject
    public ClanXpContributionRepository(Database database) {
        this.database = database;
    }

    /**
     * Loads all XP contribution records for the given clan.
     * Called once per clan during server startup.
     *
     * @return map of UUID-string -> total XP contributed
     */
    public Map<String, Double> getContributions(Clan clan) {
        Map<String, Double> result = new HashMap<>();
        try {
            Result<Record2<String, Double>> records = database.getDslContext()
                    .select(field(name("member"), String.class), field(name("contribution"), Double.class))
                    .from(table(name("clan_xp_contributions")))
                    .where(field(name("clan")).eq(clan.getId()))
                    .fetch();

            for (Record2<String, Double> record : records) {
                result.put(record.value1(), record.value2());
            }
        } catch (DataAccessException ex) {
            log.error("Failed to load XP contributions for clan {}", clan.getId(), ex).submit();
        }
        return result;
    }

    /**
     * Upserts the contribution amount for a member within a clan asynchronously.
     * Adds {@code amount} to any existing value rather than replacing it,
     * matching the in-memory {@code addContribution} behaviour.
     */
    public void saveContribution(Clan clan, UUID member, double amount) {
        CompletableFuture.runAsync(() -> {
            try {
                database.getDslContext()
                        .insertInto(table(name("clan_xp_contributions")))
                        .set(field(name("clan")), clan.getId())
                        .set(field(name("member")), member.toString())
                        .set(field(name("contribution")), amount)
                        .onDuplicateKeyUpdate()
                        .set(field(name("contribution"), Double.class),
                                field(name("clan_xp_contributions.contribution"), Double.class).plus(amount))
                        .execute();
            } catch (DataAccessException ex) {
                log.error("Failed to save XP contribution for clan {} member {}", clan.getId(), member, ex).submit();
            }
        });
    }

    /**
     * Deletes all contribution records for a clan. Called on clan disband.
     * Cascade is handled by the FK constraint, but this provides an explicit hook.
     */
    public void deleteContributions(Clan clan) {
        CompletableFuture.runAsync(() -> {
            try {
                database.getDslContext()
                        .deleteFrom(table(name("clan_xp_contributions")))
                        .where(field(name("clan")).eq(clan.getId()))
                        .execute();
            } catch (DataAccessException ex) {
                log.error("Failed to delete XP contributions for clan {}", clan.getId(), ex).submit();
            }
        });
    }

}
