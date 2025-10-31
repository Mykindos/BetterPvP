package me.mykindos.betterpvp.champions.champions.builds.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CHAMPIONS_BUILDS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;

@Singleton
@CustomLog
public class BuildRepository implements IRepository<RoleBuild> {

    private final Database database;
    private final ChampionsSkillManager skillManager;

    @Inject
    public BuildRepository(Database database, ChampionsSkillManager skillManager) {
        this.database = database;
        this.skillManager = skillManager;
    }

    @Override
    public List<RoleBuild> getAll() {
        return new ArrayList<>();
    }

    public void loadBuilds(GamerBuilds builds) {

        try {
            DSLContext ctx = database.getDslContext();
            Result<Record> results = ctx.select(CHAMPIONS_BUILDS.asterisk(), CLIENTS.UUID)
                    .from(CHAMPIONS_BUILDS).join(CLIENTS).on(CHAMPIONS_BUILDS.CLIENT.eq(CLIENTS.ID))
                    .where(CHAMPIONS_BUILDS.CLIENT.eq(builds.getClient().getId())).fetch();
            results.forEach(buildRecord -> {
                long clientId = buildRecord.getValue(CHAMPIONS_BUILDS.CLIENT);
                String clientUUID = buildRecord.getValue(CLIENTS.UUID);
                String role = buildRecord.getValue(CHAMPIONS_BUILDS.ROLE);
                int id = buildRecord.getValue(CHAMPIONS_BUILDS.ID);
                RoleBuild build = new RoleBuild(clientId, UUID.fromString(clientUUID), Role.valueOf(role.toUpperCase()), id);

                boolean active = buildRecord.getValue(CHAMPIONS_BUILDS.ACTIVE) == 1;
                build.setActive(active);

                String sword = buildRecord.getValue(CHAMPIONS_BUILDS.SWORD);
                setSkill(build, SkillType.SWORD, sword);

                String axe = buildRecord.getValue(CHAMPIONS_BUILDS.AXE);
                setSkill(build, SkillType.AXE, axe);

                String bow = buildRecord.getValue(CHAMPIONS_BUILDS.BOW);
                setSkill(build, SkillType.BOW, bow);

                String passiveA = buildRecord.getValue(CHAMPIONS_BUILDS.PASSIVE_A);
                setSkill(build, SkillType.PASSIVE_A, passiveA);

                String passiveB = buildRecord.getValue(CHAMPIONS_BUILDS.PASSIVE_B);
                setSkill(build, SkillType.PASSIVE_B, passiveB);

                String global = buildRecord.getValue(CHAMPIONS_BUILDS.GLOBAL);
                setSkill(build, SkillType.GLOBAL, global);

                if (active) {
                    builds.getActiveBuilds().put(role, build);
                }

                builds.getBuilds().add(build);
            });


        } catch (DataAccessException ex) {
            log.error("Failed to load builds", ex).submit();
        }

    }

    private void setSkill(RoleBuild build, SkillType type, String value) {

        if (value != null && !value.isEmpty()) {
            String[] split = value.split(",");
            setSkill(build, type, split[0], Integer.parseInt(split[1]));
        }
    }

    private void setSkill(RoleBuild build, SkillType type, String skillName, int level) {
        Skill skill = skillManager.getObjects().get(skillName);
        if (skill == null) return;
        if (!skill.isEnabled()) {
            if (!build.isActive()) return;
            Player player = Bukkit.getPlayer(build.getClientUUID());
            if (player == null) return;
            UtilMessage.message(player, "Champions", UtilMessage.deserialize("<green>%s</green> has been disabled on this server, refunding <green>%s</green> skill point(s) and removing from <yellow>%s</yellow> build <green>%s</green>", skill.getName(), level, build.getRole().toString(), build.getId()));
            return;
        }

        if (skill.getType() != type) {
            return;
        }

        if (level > skill.getMaxLevel()) {
            level = skill.getMaxLevel();
        }

        build.setSkill(type, skill, level);
        build.takePoints(level);
    }

    @Override
    public void save(RoleBuild build) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.insertInto(CHAMPIONS_BUILDS)
                    .set(CHAMPIONS_BUILDS.CLIENT, build.getClientId())
                    .set(CHAMPIONS_BUILDS.ROLE, build.getRole().getName())
                    .set(CHAMPIONS_BUILDS.ID, build.getId())
                    .set(CHAMPIONS_BUILDS.SWORD, getSkillDatabaseValue(build.getSwordSkill()))
                    .set(CHAMPIONS_BUILDS.AXE, getSkillDatabaseValue(build.getAxeSkill()))
                    .set(CHAMPIONS_BUILDS.BOW, getSkillDatabaseValue(build.getBow()))
                    .set(CHAMPIONS_BUILDS.PASSIVE_A, getSkillDatabaseValue(build.getPassiveA()))
                    .set(CHAMPIONS_BUILDS.PASSIVE_B, getSkillDatabaseValue(build.getPassiveB()))
                    .set(CHAMPIONS_BUILDS.GLOBAL, getSkillDatabaseValue(build.getGlobal()))
                    .set(CHAMPIONS_BUILDS.ACTIVE, build.isActive() ? 1 : 0)
                    .onConflict(CHAMPIONS_BUILDS.CLIENT, CHAMPIONS_BUILDS.ROLE, CHAMPIONS_BUILDS.ID).doNothing()
                    .execute();
        });
    }

    private String getSkillDatabaseValue(BuildSkill skill) {
        return skill == null ? null : skill.getSkill().getName() + "," + skill.getLevel();
    }

    public void update(RoleBuild build) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.insertInto(CHAMPIONS_BUILDS)
                    .set(CHAMPIONS_BUILDS.CLIENT, build.getClientId())
                    .set(CHAMPIONS_BUILDS.ROLE, build.getRole().getName())
                    .set(CHAMPIONS_BUILDS.ID, build.getId())
                    .set(CHAMPIONS_BUILDS.SWORD, getSkillDatabaseValue(build.getSwordSkill()))
                    .set(CHAMPIONS_BUILDS.AXE, getSkillDatabaseValue(build.getAxeSkill()))
                    .set(CHAMPIONS_BUILDS.BOW, getSkillDatabaseValue(build.getBow()))
                    .set(CHAMPIONS_BUILDS.PASSIVE_A, getSkillDatabaseValue(build.getPassiveA()))
                    .set(CHAMPIONS_BUILDS.PASSIVE_B, getSkillDatabaseValue(build.getPassiveB()))
                    .set(CHAMPIONS_BUILDS.GLOBAL, getSkillDatabaseValue(build.getGlobal()))
                    .set(CHAMPIONS_BUILDS.ACTIVE, build.isActive() ? 1 : 0 )
                    .onConflict(CHAMPIONS_BUILDS.CLIENT, CHAMPIONS_BUILDS.ROLE, CHAMPIONS_BUILDS.ID)
                    .doUpdate()
                    .set(CHAMPIONS_BUILDS.SWORD, getSkillDatabaseValue(build.getSwordSkill()))
                    .set(CHAMPIONS_BUILDS.AXE, getSkillDatabaseValue(build.getAxeSkill()))
                    .set(CHAMPIONS_BUILDS.BOW, getSkillDatabaseValue(build.getBow()))
                    .set(CHAMPIONS_BUILDS.PASSIVE_A, getSkillDatabaseValue(build.getPassiveA()))
                    .set(CHAMPIONS_BUILDS.PASSIVE_B, getSkillDatabaseValue(build.getPassiveB()))
                    .set(CHAMPIONS_BUILDS.GLOBAL, getSkillDatabaseValue(build.getGlobal()))
                    .set(CHAMPIONS_BUILDS.ACTIVE, build.isActive() ? 1 : 0)
                    .execute();

            log.info("Saved build for {} role {}", build.getClientId(), build.getRole().getName()).submit();
        });
    }

    public void loadDefaultBuilds(GamerBuilds gamerBuilds) {

        if (!gamerBuilds.getBuilds().isEmpty()) return;

        long clientId = gamerBuilds.getClient().getId();
        UUID uuid = UUID.fromString(gamerBuilds.getClient().getUuid());

        List<RoleBuild> builds = new ArrayList<>();
        for (int d = 1; d < 5; d++) {

            RoleBuild assassin = new RoleBuild(clientId, uuid, Role.valueOf("ASSASSIN"), d);
            setSkill(assassin, SkillType.SWORD, "Sever", 3);
            setSkill(assassin, SkillType.AXE, "Leap", 5);
            setSkill(assassin, SkillType.PASSIVE_A, "Smoke Bomb", 3);
            setSkill(assassin, SkillType.PASSIVE_B, "Backstab", 1);

            RoleBuild brute = new RoleBuild(clientId, uuid, Role.valueOf("BRUTE"), d);
            setSkill(brute, SkillType.SWORD, "Flesh Hook", 3);
            setSkill(brute, SkillType.AXE, "Seismic Slam", 5);
            setSkill(brute, SkillType.PASSIVE_A, "Stampede", 3);
            setSkill(brute, SkillType.PASSIVE_B, "Colossus", 1);

            RoleBuild ranger = new RoleBuild(clientId, uuid, Role.valueOf("RANGER"), d);
            setSkill(ranger, SkillType.SWORD, "Disengage", 3);
            setSkill(ranger, SkillType.AXE, "Wind Burst", 1);
            setSkill(ranger, SkillType.BOW, "Napalm Shot", 4);
            setSkill(ranger, SkillType.PASSIVE_B, "Sharpshooter", 3);
            setSkill(ranger, SkillType.PASSIVE_A, "Hunters Thrill", 1);

            RoleBuild mage = new RoleBuild(clientId, uuid, Role.valueOf("MAGE"), d);
            setSkill(mage, SkillType.SWORD, "Inferno", 5);
            setSkill(mage, SkillType.AXE, "Fire Blast", 3);
            setSkill(mage, SkillType.PASSIVE_A, "Immolate", 2);
            setSkill(mage, SkillType.PASSIVE_B, "Holy Light", 2);

            RoleBuild knight = new RoleBuild(clientId, uuid, Role.valueOf("KNIGHT"), d);
            setSkill(knight, SkillType.SWORD, "Riposte", 3);
            setSkill(knight, SkillType.AXE, "Bulls Charge", 5);
            setSkill(knight, SkillType.PASSIVE_A, "Swordsmanship", 1);
            setSkill(knight, SkillType.PASSIVE_B, "Vengeance", 3);

            RoleBuild warlock = new RoleBuild(clientId, uuid, Role.valueOf("WARLOCK"), d);
            setSkill(warlock, SkillType.SWORD, "Leech", 4);
            setSkill(warlock, SkillType.AXE, "Bloodshed", 5);
            setSkill(warlock, SkillType.PASSIVE_A, "Frailty", 1);
            setSkill(warlock, SkillType.PASSIVE_B, "Soul Harvest", 2);

            builds.addAll(List.of(knight, ranger, brute, mage, assassin, warlock));

        }

        builds.forEach(build -> {
            if (build.getId() == 1) {
                build.setActive(true);
            }

            save(build);
            gamerBuilds.getBuilds().add(build);
            if (build.isActive()) {
                gamerBuilds.getActiveBuilds().put(build.getRole().getName(), build);
            }
        });
    }

}
