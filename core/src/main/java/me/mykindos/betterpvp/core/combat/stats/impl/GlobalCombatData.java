package me.mykindos.betterpvp.core.combat.stats.impl;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.FloatStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
public class GlobalCombatData extends CombatData {

    public GlobalCombatData(UUID holder) {
        super(holder);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        List<Statement> killStatements = new ArrayList<>();
        List<Statement> contributionStatements = new ArrayList<>();
        final String killStmt = "INSERT INTO kills (Id, Server, Season, Killer, Victim, Contribution, Damage, RatingDelta, Time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        final String assistStmt = "INSERT INTO kill_contributions (KillId, Id, Contributor, Contribution, Damage) VALUES (?, ?, ?, ?, ?);";

        // Add kills - This is done first because it if it breaks it will stop the rating update
        // Kills are only saved by the victim
        for (final Kill kill : pendingKills) {
            final UUID killId = kill.getId();
            final List<Contribution> contributions = kill.getContributions();

            final Contribution killerContribution = contributions.stream().filter(contribution -> contribution.getContributor().equals(kill.getKiller())).findFirst().orElseThrow();
            Statement killStatement = new Statement(killStmt,
                    new UuidStatementValue(killId),
                    new StringStatementValue(Core.getCurrentServer()),
                    new StringStatementValue(Core.getCurrentSeason()),
                    new UuidStatementValue(kill.getKiller()),
                    new UuidStatementValue(kill.getVictim()),
                    new FloatStatementValue(killerContribution.getPercentage()),
                    new FloatStatementValue(killerContribution.getDamage()),
                    new IntegerStatementValue(kill.getRatingDelta()),
                    new LongStatementValue(kill.getTime()));
            killStatements.add(killStatement);

            contributions.remove(killerContribution);
            for (final Contribution contribution : contributions) {
                Statement assistStatement = new Statement(assistStmt,
                        new UuidStatementValue(killId),
                        new UuidStatementValue(contribution.getId()),
                        new UuidStatementValue(contribution.getContributor()),
                        new FloatStatementValue(contribution.getPercentage()),
                        new FloatStatementValue(contribution.getDamage()));
                contributionStatements.add(assistStatement);
            }
        }

        // Save attachments
        for (ICombatDataAttachment attachment : attachments) {
            attachment.prepareUpdates(this, database);
        }
        // Save self-rating (this saves independently for each player)
        String ratingStmt = "INSERT INTO combat_stats (Client, Server, Season, Rating, Killstreak, HighestKillstreak) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Rating = VALUES(Rating), Killstreak = VALUES(Killstreak), HighestKillstreak = VALUES(HighestKillstreak)";
        Statement victimRating = new Statement(ratingStmt,
                new UuidStatementValue(getHolder()),
                new StringStatementValue(Core.getCurrentServer()),
                new StringStatementValue(Core.getCurrentSeason()),
                new IntegerStatementValue(getRating()),
                new IntegerStatementValue(getKillStreak()),
                new IntegerStatementValue(getHighestKillStreak()));

        CompletableFuture<Void> killsFuture = database.executeBatch(killStatements, TargetDatabase.GLOBAL);

        killsFuture.thenRun(() -> {
                    database.executeBatch(contributionStatements, TargetDatabase.GLOBAL);
                }).thenRun(() -> {
                    database.executeUpdate(victimRating, TargetDatabase.GLOBAL);
                }).thenRun(pendingKills::clear)
                .exceptionally(ex -> {
                    log.error("Failed to save combat data", ex).submit();
                    return null;
                });

    }
}
