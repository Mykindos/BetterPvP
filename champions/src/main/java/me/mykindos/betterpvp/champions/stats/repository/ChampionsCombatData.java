package me.mykindos.betterpvp.champions.stats.repository;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChampionsCombatData extends CombatData {

    private final RoleManager roleManager;
    private final @Nullable Role role;

    public ChampionsCombatData(UUID holder, RoleManager roleManager, @Nullable Role role) {
        super(holder);
        this.roleManager = roleManager;
        this.role = role;
    }

    @Override
    protected ChampionsKill generateKill(UUID killer, UUID victim, int ratingDelta, List<Contribution> contributions) {
        final Role killerRole = roleManager.getObject(killer).orElse(null);
        final Role victimRole = roleManager.getObject(victim).orElse(null);
        final Map<Contribution, Role> contributorRoles = contributions.stream().collect(Collectors.toMap(
                contribution -> contribution, contribution -> roleManager.getObject(contribution.getContributor()).orElse(null)
        ));
        return new ChampionsKill(killer, victim, ratingDelta, contributions, killerRole, victimRole, contributorRoles);
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
                    new StringStatementValue(championsKill.getKillerRole().toString()),
                    new StringStatementValue(championsKill.getVictimRole().toString()));
            killStatements.add(killStatement);

            contributions.entrySet().removeIf(entry -> entry.getKey().getContributor() == kill.getKiller());
            contributions.forEach((contribution, cRole) -> {
                Statement assistStatement = new Statement(assistStmt,
                        new UuidStatementValue(contribution.getId()),
                        new StringStatementValue(cRole.toString()));
                contributionStatements.add(assistStatement);
            });
        }

        // Save attachments
        attachments.forEach(attachment -> attachment.prepareUpdates(this, database, databasePrefix));

        // Save self-rating (this saves independently for each player)
        String ratingStmt = "INSERT INTO " + databasePrefix + "ratings (Gamer, Class, Rating) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Rating = VALUES(Rating);";
        Statement victimRating = new Statement(ratingStmt,
                new UuidStatementValue(getHolder()),
                new StringStatementValue(role == null ? null : role.toString()),
                new IntegerStatementValue(getRating()));

        database.executeBatch(killStatements, false);
        database.executeBatch(contributionStatements, false);
        database.executeUpdate(victimRating);
        pendingKills.clear();
    }

}
