package me.mykindos.betterpvp.champions.stats.repository;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChampionsCombatData extends CombatData {

    private final RoleManager roleManager;
    private final @Nullable Role role;

    public ChampionsCombatData(UUID holder, RoleManager roleManager, @Nullable Role role) {
        super(holder);
        this.roleManager = roleManager;
        this.role = role;
    }

    public @Nullable Role getRole() {
        return role;
    }

    @Override
    protected ChampionsKill generateKill(UUID killId, UUID killer, UUID victim, int ratingDelta, List<Contribution> contributions) {
        final Role killerRole = roleManager.getObject(killer).orElse(null);
        final Role victimRole = roleManager.getObject(victim).orElse(null);
        final Map<Contribution, Role> contributorRoles = new HashMap<>();
        contributions.forEach(contribution -> {
            final Role contributorRole = roleManager.getObject(contribution.getContributor()).orElse(null);
            contributorRoles.put(contribution, contributorRole);
        });
        return new ChampionsKill(killId, killer, victim, ratingDelta, contributions, killerRole, victimRole, contributorRoles);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix) {
        List<Statement> killStatements = new ArrayList<>();
        List<Statement> contributionStatements = new ArrayList<>();
        final String killStmt = "INSERT INTO " + databasePrefix + "kills (KillId, KillerClass, VictimClass) VALUES (?, ?, ?);";
        final String assistStmt = "INSERT INTO " + databasePrefix + "kill_contributions (ContributionId, ContributorClass) VALUES (?, ?);";

        // Add kills - This is done first because it if it breaks it will stop the rating update
        // Kills are only saved by the victim
        for (final Kill kill: pendingKills) {
            final ChampionsKill championsKill = (ChampionsKill) kill;
            final UUID killId = kill.getId();
            final Map<Contribution, Role> contributions = championsKill.getContributionRoles();

            Statement killStatement = new Statement(killStmt,
                    new UuidStatementValue(killId),
                    new StringStatementValue(championsKill.getKillerRole() == null ? "" : championsKill.getKillerRole().toString()),
                    new StringStatementValue(championsKill.getVictimRole() == null ? "" : championsKill.getVictimRole().toString()));
            killStatements.add(killStatement);

            contributions.entrySet().removeIf(entry -> entry.getKey().getContributor() == kill.getKiller());
            contributions.forEach((contribution, cRole) -> {
                Statement assistStatement = new Statement(assistStmt,
                        new UuidStatementValue(contribution.getId()),
                        new StringStatementValue(cRole == null ? "" : cRole.toString()));
                contributionStatements.add(assistStatement);
            });
        }

        // Save attachments
        for (ICombatDataAttachment attachment : attachments) {
            attachment.prepareUpdates(this, database, databasePrefix);
        }

        // Save self-rating (this saves independently for each player)
        String ratingStmt = "INSERT INTO " + databasePrefix + "combat_stats (Gamer, Class, Rating, Killstreak, HighestKillstreak) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE Rating = VALUES(Rating), Killstreak = VALUES(Killstreak), HighestKillstreak = VALUES(HighestKillstreak);";
        Statement victimRating = new Statement(ratingStmt,
                new UuidStatementValue(getHolder()),
                new StringStatementValue(role == null ? "" : role.toString()),
                new IntegerStatementValue(getRating()),
                new IntegerStatementValue(getKillStreak()),
                new IntegerStatementValue(getHighestKillStreak()));

        database.executeBatch(killStatements, false);
        database.executeBatch(contributionStatements, false);
        database.executeUpdate(victimRating);
        pendingKills.clear();
    }

}
