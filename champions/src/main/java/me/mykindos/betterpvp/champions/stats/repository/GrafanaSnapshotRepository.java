package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.math.BigDecimal;

import static me.mykindos.betterpvp.champions.database.jooq.Tables.CHAMPIONS_KILLS;
import static me.mykindos.betterpvp.champions.database.jooq.Tables.GRAFANA_ROLE_PLAYTIME_SNAPSHOT;
import static me.mykindos.betterpvp.champions.database.jooq.Tables.GRAFANA_SKILL_KDR_SNAPSHOT;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENT_STATS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.KILLS;

/**
 * Periodically snapshots Champions analytics data into dedicated Grafana tables.
 *
 * <p>Each snapshot captures the <em>cumulative</em> realm totals at that point in time,
 * letting Grafana show both the current state (latest snapshot) and historical trends
 * (time-series over all snapshots).</p>
 *
 * <p>Scheduled in {@link me.mykindos.betterpvp.champions.Champions#onEnable()} via
 * {@code UtilServer.runTaskTimerAsync}.</p>
 */
@Singleton
@CustomLog
public class GrafanaSnapshotRepository {

    private final Database database;

    @Inject
    public GrafanaSnapshotRepository(Database database) {
        this.database = database;
    }

    /**
     * Inserts one snapshot row per tracked entity (role matchup, role playtime, skill)
     * for the given realm into the three Grafana snapshot tables.
     *
     * @param realmId the current realm ID (from {@link me.mykindos.betterpvp.core.Core#getCurrentRealm()})
     */
    public void takeSnapshot(int realmId) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            takeRolePlaytimeSnapshot(ctx, realmId);
            takeSkillKdrSnapshot(ctx, realmId);
            log.info("Grafana snapshot taken for realm {}", realmId).submit();
        }).exceptionally(ex -> {
            log.error("Failed to take Grafana snapshot for realm {}", realmId, ex).submit();
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void takeRolePlaytimeSnapshot(DSLContext ctx, int realmId) {
        // JSONB path accessors
        Field<String> wrappedStatType = DSL.field("{0}->'wrappedStat'->>'statType'", String.class, CLIENT_STATS.STATDATA);
        Field<String> wrappedAction   = DSL.field("{0}->'wrappedStat'->>'action'",   String.class, CLIENT_STATS.STATDATA);
        Field<String> wrappedRole     = DSL.field("{0}->'wrappedStat'->>'role'",     String.class, CLIENT_STATS.STATDATA);

        // CTE 1: time played per role
        CommonTableExpression<?> roleTime = DSL.name("role_time")
                .fields("role", "time_played_ms")
                .as(ctx.select(wrappedRole, DSL.sum(CLIENT_STATS.STAT))
                        .from(CLIENT_STATS)
                        .where(CLIENT_STATS.STATTYPE.eq("CLANS_WRAPPER"))
                        .and(wrappedStatType.eq("CHAMPIONS_ROLE"))
                        .and(wrappedAction.eq("TIME_PLAYED"))
                        .and(CLIENT_STATS.REALM.eq(realmId))
                        .groupBy(wrappedRole));

        // CTE 2: kills per role
        CommonTableExpression<?> roleKills = DSL.name("role_kills")
                .fields("role", "kills")
                .as(ctx.select(CHAMPIONS_KILLS.KILLER_CLASS, DSL.count().cast(Long.class))
                        .from(KILLS)
                        .join(CHAMPIONS_KILLS).on(KILLS.ID.eq(CHAMPIONS_KILLS.KILL_ID))
                        .where(KILLS.REALM.eq(realmId))
                        .and(KILLS.VALID.isTrue())
                        .and(CHAMPIONS_KILLS.KILLER_CLASS.ne(""))
                        .groupBy(CHAMPIONS_KILLS.KILLER_CLASS));

        // CTE 3: deaths per role
        CommonTableExpression<?> roleDeaths = DSL.name("role_deaths")
                .fields("role", "deaths")
                .as(ctx.select(CHAMPIONS_KILLS.VICTIM_CLASS, DSL.count().cast(Long.class))
                        .from(KILLS)
                        .join(CHAMPIONS_KILLS).on(KILLS.ID.eq(CHAMPIONS_KILLS.KILL_ID))
                        .where(KILLS.REALM.eq(realmId))
                        .and(KILLS.VALID.isTrue())
                        .and(CHAMPIONS_KILLS.VICTIM_CLASS.ne(""))
                        .groupBy(CHAMPIONS_KILLS.VICTIM_CLASS));

        Field<String>     tRole        = DSL.field(DSL.name("t", "role"),          String.class);
        Field<Long>       kKills       = DSL.field(DSL.name("k", "kills"),          Long.class);
        Field<Long>       dDeaths      = DSL.field(DSL.name("d", "deaths"),         Long.class);
        Field<Long>       tTimePlayed  = DSL.field(DSL.name("t", "time_played_ms"), Long.class);
        Field<String>     kRole        = DSL.field(DSL.name("k", "role"),           String.class);
        Field<String>     dRole        = DSL.field(DSL.name("d", "role"),           String.class);

        Field<Long>       kKillsOrZero  = DSL.coalesce(kKills,  0L);
        Field<Long>       dDeathsOrZero = DSL.coalesce(dDeaths, 0L);
        Field<BigDecimal> kdr = DSL
                .when(dDeathsOrZero.eq(0L), kKillsOrZero.cast(SQLDataType.NUMERIC))
                .otherwise(DSL.round(kKillsOrZero.cast(SQLDataType.NUMERIC).div(dDeaths), 2));

        ctx.with(roleTime, roleKills, roleDeaths)
                .insertInto(GRAFANA_ROLE_PLAYTIME_SNAPSHOT,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.REALM,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.CAPTURED_AT,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.ROLE,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.KILLS,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.DEATHS,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.KDR,
                        GRAFANA_ROLE_PLAYTIME_SNAPSHOT.TIME_PLAYED_MS)
                .select(DSL.select(
                                DSL.val(realmId),
                                DSL.field("NOW()", SQLDataType.TIMESTAMPWITHTIMEZONE),
                                tRole,
                                kKillsOrZero,
                                dDeathsOrZero,
                                kdr,
                                tTimePlayed)
                        .from(DSL.table(DSL.name("role_time")).as("t"))
                        .leftJoin(DSL.table(DSL.name("role_kills")).as("k")).on(tRole.eq(kRole))
                        .leftJoin(DSL.table(DSL.name("role_deaths")).as("d")).on(tRole.eq(dRole)))
                .execute();
    }

    private void takeSkillKdrSnapshot(DSLContext ctx, int realmId) {
        // JSONB path accessors
        Field<String> wrappedStatType  = DSL.field("{0}->'wrappedStat'->>'statType'",  String.class, CLIENT_STATS.STATDATA);
        Field<String> wrappedAction    = DSL.field("{0}->'wrappedStat'->>'action'",    String.class, CLIENT_STATS.STATDATA);
        Field<String> wrappedSkillName = DSL.field("{0}->'wrappedStat'->>'skillName'", String.class, CLIENT_STATS.STATDATA);

        // Base WHERE conditions shared by all three skill CTEs
        var baseCondition = CLIENT_STATS.STATTYPE.eq("CLANS_WRAPPER")
                .and(wrappedStatType.eq("CHAMPIONS_SKILL"))
                .and(wrappedSkillName.isNotNull())
                .and(wrappedSkillName.ne(""))
                .and(CLIENT_STATS.REALM.eq(realmId));

        // CTE 1: kills per skill
        CommonTableExpression<?> skillKills = DSL.name("skill_kills")
                .fields("skill_name", "kills")
                .as(ctx.select(wrappedSkillName, DSL.sum(CLIENT_STATS.STAT))
                        .from(CLIENT_STATS)
                        .where(baseCondition.and(wrappedAction.eq("KILL")))
                        .groupBy(wrappedSkillName));

        // CTE 2: deaths per skill
        CommonTableExpression<?> skillDeaths = DSL.name("skill_deaths")
                .fields("skill_name", "deaths")
                .as(ctx.select(wrappedSkillName, DSL.sum(CLIENT_STATS.STAT))
                        .from(CLIENT_STATS)
                        .where(baseCondition.and(wrappedAction.eq("DEATH")))
                        .groupBy(wrappedSkillName));

        // CTE 3: time played per skill
        CommonTableExpression<?> skillTime = DSL.name("skill_time")
                .fields("skill_name", "time_played_ms")
                .as(ctx.select(wrappedSkillName, DSL.sum(CLIENT_STATS.STAT))
                        .from(CLIENT_STATS)
                        .where(baseCondition.and(wrappedAction.eq("TIME_PLAYED")))
                        .groupBy(wrappedSkillName));

        Field<String>     kSkillName       = DSL.field(DSL.name("k", "skill_name"),    String.class);
        Field<Long>       kKills           = DSL.field(DSL.name("k", "kills"),          Long.class);
        Field<Long>       dDeaths          = DSL.field(DSL.name("d", "deaths"),         Long.class);
        Field<Long>       tTimePlayed      = DSL.field(DSL.name("t", "time_played_ms"), Long.class);
        Field<String>     dSkillName       = DSL.field(DSL.name("d", "skill_name"),     String.class);
        Field<String>     tSkillName       = DSL.field(DSL.name("t", "skill_name"),     String.class);

        Field<Long>       dDeathsOrZero    = DSL.coalesce(dDeaths,     0L);
        Field<Long>       tTimePlayedOrZero = DSL.coalesce(tTimePlayed, 0L);
        Field<BigDecimal> kdr = DSL
                .when(dDeathsOrZero.eq(0L), kKills.cast(SQLDataType.NUMERIC))
                .otherwise(DSL.round(kKills.cast(SQLDataType.NUMERIC).div(dDeaths), 2));

        ctx.with(skillKills, skillDeaths, skillTime)
                .insertInto(GRAFANA_SKILL_KDR_SNAPSHOT,
                        GRAFANA_SKILL_KDR_SNAPSHOT.REALM,
                        GRAFANA_SKILL_KDR_SNAPSHOT.CAPTURED_AT,
                        GRAFANA_SKILL_KDR_SNAPSHOT.SKILL_NAME,
                        GRAFANA_SKILL_KDR_SNAPSHOT.KILLS,
                        GRAFANA_SKILL_KDR_SNAPSHOT.DEATHS,
                        GRAFANA_SKILL_KDR_SNAPSHOT.KDR,
                        GRAFANA_SKILL_KDR_SNAPSHOT.TIME_PLAYED_MS)
                .select(DSL.select(
                                DSL.val(realmId),
                                DSL.field("NOW()", SQLDataType.TIMESTAMPWITHTIMEZONE),
                                kSkillName,
                                kKills,
                                dDeathsOrZero,
                                kdr,
                                tTimePlayedOrZero)
                        .from(DSL.table(DSL.name("skill_kills")).as("k"))
                        .leftJoin(DSL.table(DSL.name("skill_deaths")).as("d")).on(kSkillName.eq(dSkillName))
                        .leftJoin(DSL.table(DSL.name("skill_time")).as("t")).on(kSkillName.eq(tSkillName)))
                .execute();
    }
}
