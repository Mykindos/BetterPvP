package me.mykindos.betterpvp.core.combat.stats.impl;

import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.FloatStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GlobalCombatData extends CombatData {

    public GlobalCombatData(UUID holder) {
        super(holder);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix) {
        List<Statement> killStatements = new ArrayList<>();
        List<Statement> contributionStatements = new ArrayList<>();
        final String killStmt = "INSERT INTO " + databasePrefix + "kills (Id, Killer, Victim, Contribution, Damage, RatingDelta, Timestamp) VALUES (?, ?, ?, ?, ?, ?, ?);";
        final String assistStmt = "INSERT INTO " + databasePrefix + "kill_contributions (KillId, Id, Contributor, Contribution, Damage) VALUES (?, ?, ?, ?, ?);";

        // Add kills - This is done first because it if it breaks it will stop the rating update
        // Kills are only saved by the victim
        for (final Kill kill: pendingKills) {
            final UUID killId = kill.getId();
            final List<Contribution> contributions = kill.getContributions();

            final Contribution killerContribution = contributions.stream().filter(contribution -> contribution.getContributor().equals(kill.getKiller())).findFirst().orElseThrow();
            Statement killStatement = new Statement(killStmt,
                    new UuidStatementValue(killId),
                    new UuidStatementValue(kill.getKiller()),
                    new UuidStatementValue(kill.getVictim()),
                    new FloatStatementValue(killerContribution.getPercentage()),
                    new FloatStatementValue(killerContribution.getDamage()),
                    new IntegerStatementValue(kill.getRatingDelta()),
                    new TimestampStatementValue(kill.getTimestamp()));
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
        attachments.forEach(attachment -> attachment.prepareUpdates(this, database, databasePrefix));

        // Save self-rating (this saves independently for each player)
        String ratingStmt = "INSERT INTO " + databasePrefix + "combat_stats (Gamer, Rating, Killstreak, HighestKillstreak) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Rating = VALUES(Rating), Killstreak = VALUES(Killstreak), HighestKillstreak = VALUES(HighestKillstreak)";
        Statement victimRating = new Statement(ratingStmt,
                new UuidStatementValue(getHolder()),
                new IntegerStatementValue(getRating()),
                new IntegerStatementValue(getKillStreak()),
                new IntegerStatementValue(getHighestKillStreak()));

        database.executeBatch(killStatements, false);
        database.executeBatch(contributionStatements, false);
        database.executeUpdate(victimRating);
        pendingKills.clear();
    }
}
