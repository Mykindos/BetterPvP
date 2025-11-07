package me.mykindos.betterpvp.champions.stats.repository;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.combat.stats.model.Kill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.champions.database.jooq.tables.records.ChampionsKillContributionsRecord;
import me.mykindos.betterpvp.champions.database.jooq.tables.records.ChampionsKillsRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.mykindos.betterpvp.champions.database.jooq.Tables.CHAMPIONS_COMBAT_STATS;
import static me.mykindos.betterpvp.champions.database.jooq.Tables.CHAMPIONS_KILLS;
import static me.mykindos.betterpvp.champions.database.jooq.Tables.CHAMPIONS_KILL_CONTRIBUTIONS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;

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
    protected ChampionsKill generateKill(long killId, UUID killer, UUID victim, int ratingDelta, List<Contribution> contributions) {
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
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        List<ChampionsKillsRecord> killRecords = new ArrayList<>();
        List<ChampionsKillContributionsRecord> contributionRecords = new ArrayList<>();

        // Add kills - This is done first because if it breaks it will stop the rating update
        // Kills are only saved by the victim
        for (final Kill kill : pendingKills) {
            final ChampionsKill championsKill = (ChampionsKill) kill;
            final Long killId = kill.getId();
            final Map<Contribution, Role> contributions = championsKill.getContributionRoles();

            // Create kill record
            ChampionsKillsRecord killRecord = database.getDslContext().newRecord(CHAMPIONS_KILLS);
            killRecord.setKillId(killId);
            killRecord.setKillerClass(championsKill.getKillerRole() == null ? "" : championsKill.getKillerRole().toString());
            killRecord.setVictimClass(championsKill.getVictimRole() == null ? "" : championsKill.getVictimRole().toString());
            killRecords.add(killRecord);

            contributions.entrySet().removeIf(entry -> entry.getKey().getContributor() == kill.getKiller());
            contributions.forEach((contribution, cRole) -> {
                ChampionsKillContributionsRecord contributionRecord = database.getDslContext().newRecord(CHAMPIONS_KILL_CONTRIBUTIONS);
                contributionRecord.setContributionId(contribution.getId());
                contributionRecord.setContributorClass(cRole == null ? "" : cRole.toString());
                contributionRecords.add(contributionRecord);
            });
        }

        // Save attachments
        for (ICombatDataAttachment attachment : attachments) {
            attachment.prepareUpdates(this, database);
        }

        // Save self-rating (this saves independently for each player)
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            // Batch insert kills
            if (!killRecords.isEmpty()) {
                ctx.batchInsert(killRecords).execute();
            }

            // Batch insert contributions
            if (!contributionRecords.isEmpty()) {
                ctx.batchInsert(contributionRecords).execute();
            }

            // Insert/update victim rating using INSERT ... ON DUPLICATE KEY UPDATE
            ctx.insertInto(CHAMPIONS_COMBAT_STATS)
                    .set(CHAMPIONS_COMBAT_STATS.CLIENT, database.getDslContext().select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(getHolder().toString())))
                    .set(CHAMPIONS_COMBAT_STATS.REALM, Core.getCurrentRealm())
                    .set(CHAMPIONS_COMBAT_STATS.CLASS, role == null ? "" : role.toString())
                    .set(CHAMPIONS_COMBAT_STATS.RATING, getRating())
                    .set(CHAMPIONS_COMBAT_STATS.KILLSTREAK, getKillStreak())
                    .set(CHAMPIONS_COMBAT_STATS.HIGHEST_KILLSTREAK, getHighestKillStreak())
                    .onDuplicateKeyUpdate()
                    .set(CHAMPIONS_COMBAT_STATS.RATING, getRating())
                    .set(CHAMPIONS_COMBAT_STATS.KILLSTREAK, getKillStreak())
                    .set(CHAMPIONS_COMBAT_STATS.HIGHEST_KILLSTREAK, getHighestKillStreak())
                    .execute();
        });

        pendingKills.clear();
    }

}
