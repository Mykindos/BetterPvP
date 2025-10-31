package me.mykindos.betterpvp.core.combat.stats.impl;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.COMBAT_STATS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.KILLS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.KILL_CONTRIBUTIONS;

@CustomLog
public class GlobalCombatData extends CombatData {

    public GlobalCombatData(UUID holder) {
        super(holder);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        List<Query> killQueries = new ArrayList<>();
        List<Query> contributionQueries = new ArrayList<>();

        // Process kills
        for (final Kill kill : pendingKills) {
            final Long killId = kill.getId();
            final List<Contribution> contributions = kill.getContributions();

            final Contribution killerContribution = contributions.stream()
                    .filter(contribution -> contribution.getContributor().equals(kill.getKiller()))
                    .findFirst()
                    .orElseThrow();

            killQueries.add(
                    database.getDslContext()
                            .insertInto(KILLS)
                            .set(KILLS.ID, killId)
                            .set(KILLS.REALM, Core.getCurrentRealm())
                            .set(KILLS.KILLER, database.getDslContext().select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(kill.getKiller().toString())))
                            .set(KILLS.VICTIM, database.getDslContext().select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(kill.getVictim().toString())))
                            .set(KILLS.CONTRIBUTION, killerContribution.getPercentage())
                            .set(KILLS.DAMAGE, killerContribution.getDamage())
                            .set(KILLS.RATING_DELTA, kill.getRatingDelta())
                            .set(KILLS.TIME, kill.getTime())
            );

            contributions.remove(killerContribution);
            for (final Contribution contribution : contributions) {
                contributionQueries.add(
                        database.getDslContext()
                                .insertInto(KILL_CONTRIBUTIONS)
                                .set(KILL_CONTRIBUTIONS.ID, contribution.getId())
                                .set(KILL_CONTRIBUTIONS.KILL_ID, killId)
                                .set(KILL_CONTRIBUTIONS.CONTRIBUTOR, database.getDslContext().select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(contribution.getContributor().toString())))
                                .set(KILL_CONTRIBUTIONS.CONTRIBUTION, contribution.getPercentage())
                                .set(KILL_CONTRIBUTIONS.DAMAGE, contribution.getDamage())
                );
            }
        }

        // Save attachments
        for (ICombatDataAttachment attachment : attachments) {
            attachment.prepareUpdates(this, database);
        }

// Prepare combat stats query with ON DUPLICATE KEY UPDATE (MySQL) or ON CONFLICT (PostgreSQL)
        Query combatStatsQuery = database.getDslContext()
                .insertInto(COMBAT_STATS)
                .set(COMBAT_STATS.CLIENT, database.getDslContext().select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(getHolder().toString())))
                .set(COMBAT_STATS.REALM, Core.getCurrentRealm())
                .set(COMBAT_STATS.RATING, getRating())
                .set(COMBAT_STATS.KILLSTREAK, getKillStreak())
                .set(COMBAT_STATS.HIGHEST_KILLSTREAK, getHighestKillStreak())
                .onDuplicateKeyUpdate()
                .set(COMBAT_STATS.RATING, getRating())
                .set(COMBAT_STATS.KILLSTREAK, getKillStreak())
                .set(COMBAT_STATS.HIGHEST_KILLSTREAK, getHighestKillStreak());

        // Execute batches sequentially
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {

            ctx.transaction(configuration -> {
                DSLContext ctxl = DSL.using(configuration);

                // Execute kills batch
                if (!killQueries.isEmpty()) {
                    ctxl.batch(killQueries).execute();
                }

                // Execute contributions batch
                if (!contributionQueries.isEmpty()) {
                    ctxl.batch(contributionQueries).execute();
                }

                // Execute combat stats upsert
                ctxl.execute(combatStatsQuery);

                pendingKills.clear();
            });

        }).exceptionally(ex -> {
            log.error("Failed to save combat data", ex).submit();
            return null;
        });
    }
}
