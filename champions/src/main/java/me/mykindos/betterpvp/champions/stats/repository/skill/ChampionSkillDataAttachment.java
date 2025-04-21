package me.mykindos.betterpvp.champions.stats.repository.skill;

import com.google.common.collect.ConcurrentHashMultiset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.repository.SkillStatementValue;
import me.mykindos.betterpvp.champions.stats.ChampionsKill;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsCombatData;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@CustomLog
public class ChampionSkillDataAttachment implements ICombatDataAttachment<ChampionsCombatData, ChampionsKill> {
    private final BuildManager buildManager;

    @Getter(AccessLevel.NONE)
    private final ConcurrentHashMultiset<ChampionsKillSkillData> pendingSkillData = ConcurrentHashMultiset.create();

    @Override
    public void prepareUpdates(@NotNull ChampionsCombatData data, @NotNull Database database) {
        log.info("preparing update").submit();
        final List<Statement> skillDataKillStatements = new ArrayList<>();
        final List<Statement> skillDataContributionStatements = new ArrayList<>();
        final String skillDataKillStmt = "INSERT INTO champions_kills_build (KillId, " +
                "KillerSword, KillerAxe, KillerBow, KillerPassiveA, KillerPassiveB, KillerGlobal, " +
                "VictimSword, VictimAxe, VictimBow, VictimPassiveA, VictimPassiveB, VictimGlobal) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String skillDataContributionStmt = "INSERT INTO champions_kill_contributions_build (ContributionId, " +
                "Sword, Axe, Bow, PassiveA, PassiveB, Global) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?)";

        for (final ChampionsKillSkillData skillData : pendingSkillData) {
            final RoleBuild killerBuild = skillData.getKillerBuild();
            final RoleBuild victimBuild = skillData.getVictimBuild();
            Statement killStatement = new Statement(skillDataKillStmt,
                    new UuidStatementValue(skillData.getKill().getId()),
                    new SkillStatementValue(killerBuild.getSwordSkill()),
                    new SkillStatementValue(killerBuild.getAxeSkill()),
                    new SkillStatementValue(killerBuild.getBow()),
                    new SkillStatementValue(killerBuild.getPassiveA()),
                    new SkillStatementValue(killerBuild.getPassiveB()),
                    new SkillStatementValue(killerBuild.getGlobal()),
                    new SkillStatementValue(victimBuild.getSwordSkill()),
                    new SkillStatementValue(victimBuild.getAxeSkill()),
                    new SkillStatementValue(victimBuild.getBow()),
                    new SkillStatementValue(victimBuild.getPassiveA()),
                    new SkillStatementValue(victimBuild.getPassiveB()),
                    new SkillStatementValue(victimBuild.getGlobal())
            );
            skillDataKillStatements.add(killStatement);
            skillData.getContributorBuild().entrySet().removeIf(entry -> entry.getKey().getContributor() == skillData.getKill().getKiller());
            skillData.getContributorBuild().forEach((contribution, build) -> {
                Statement contributionStatement = new Statement(skillDataContributionStmt,
                        new UuidStatementValue(contribution.getId()),
                        new SkillStatementValue(build.getSwordSkill()),
                        new SkillStatementValue(build.getAxeSkill()),
                        new SkillStatementValue(build.getBow()),
                        new SkillStatementValue(build.getPassiveA()),
                        new SkillStatementValue(build.getPassiveB()),
                        new SkillStatementValue(build.getGlobal())
                        );
                skillDataContributionStatements.add(contributionStatement);
            });
        }

        database.executeBatch(skillDataKillStatements, false);
        database.executeBatch(skillDataContributionStatements, false);
        pendingSkillData.clear();

    }

    @Override
    public void onKill(@NotNull ChampionsCombatData data, ChampionsKill kill) {
        final RoleBuild killerBuild = getActiveBuild(kill.getKiller(), kill.getKillerRole());
        final RoleBuild victimBuild = getActiveBuild(kill.getVictim(), kill.getVictimRole());
        final Map<Contribution, RoleBuild> contributorRoleBuilds = new HashMap<>();
        kill.getContributionRoles().forEach((contribution, role) -> {
            final RoleBuild roleBuild = getActiveBuild(contribution.getContributor(), role);
            contributorRoleBuilds.put(contribution, roleBuild);
        });
        ChampionsKillSkillData skillData = new ChampionsKillSkillData(kill, killerBuild, victimBuild, contributorRoleBuilds);
        log.info(skillData.toString()).submit();
        pendingSkillData.add(skillData);
    }

    /**
     * Gets the active build
     * @param player
     * @param role
     * @return
     */
    @Nullable
    private RoleBuild getActiveBuild(UUID player, @Nullable Role role) {
        if (role == null) return null;
        return buildManager.getObject(player).orElseThrow()
                .getActiveBuilds().get(role.getName());
    }
}
