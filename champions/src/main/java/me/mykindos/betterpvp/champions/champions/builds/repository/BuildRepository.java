package me.mykindos.betterpvp.champions.champions.builds.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class BuildRepository implements IRepository<RoleBuild> {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
    private String tablePrefix;

    private final Database database;
    private final SkillManager skillManager;

    private final RoleManager roleManager;

    @Inject
    public BuildRepository(Database database, SkillManager skillManager, RoleManager roleManager) {
        this.database = database;
        this.skillManager = skillManager;
        this.roleManager = roleManager;
    }

    @Override
    public List<RoleBuild> getAll() {

        return null;
    }

    public void loadBuilds(GamerBuilds builds) {
        String query = "SELECT * FROM " + tablePrefix + "builds WHERE Gamer = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(builds.getUuid())));
        try {
            while (result.next()) {
                String uuid = result.getString(1);
                String role = result.getString(2);
                int id = result.getInt(3);
                Optional<Role> roleOptional = roleManager.getRole(role);
                if (roleOptional.isEmpty()) continue;
                RoleBuild build = new RoleBuild(uuid, roleOptional.get(), id);


                Role roleClass = roleOptional.get();
                String sword = result.getString(4);
                String[] swordSplit = sword.split(",");
                if (roleClass.hasSkill(SkillType.SWORD, swordSplit[0])) {
                    setSkill(build, SkillType.SWORD, sword);
                } else {
                    setSkill(build, SkillType.SWORD, null);
                }

                String axe = result.getString(5);
                if (roleClass.hasSkill(SkillType.AXE, axe.split(",")[0])) {
                    setSkill(build, SkillType.AXE, axe);
                } else {
                    setSkill(build, SkillType.AXE, null);
                }

                String bow = result.getString(6);
                if (roleClass.hasSkill(SkillType.BOW, bow.split(",")[0])) {
                    setSkill(build, SkillType.BOW, sword);
                } else {
                    setSkill(build, SkillType.BOW, null);
                }

                String passiveA = result.getString(7);
                if (roleClass.hasSkill(SkillType.PASSIVE_A, passiveA.split(",")[0])) {
                    setSkill(build, SkillType.PASSIVE_A, sword);
                } else {
                    setSkill(build, SkillType.PASSIVE_A, null);
                }


                String passiveB = result.getString(8);
                if (roleClass.hasSkill(SkillType.PASSIVE_B, passiveB.split(",")[0])) {
                    setSkill(build, SkillType.PASSIVE_B, sword);
                } else {
                    setSkill(build, SkillType.PASSIVE_B, null);
                }


                String global = result.getString(9);
                if (roleClass.hasSkill(SkillType.GLOBAL, global.split(",")[0])) {
                    setSkill(build, SkillType.GLOBAL, sword);
                } else {
                    setSkill(build, SkillType.GLOBAL, null);
                }


                boolean active = result.getBoolean(10);
                build.setActive(active);

                if (active) {
                    builds.getActiveBuilds().put(role, build);
                }

                builds.getBuilds().add(build);

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private void setSkill(RoleBuild build, SkillType type, String value) {

        if (value != null && !value.equals("")) {
            String[] split = value.split(",");
            Skill skill = skillManager.getObjects().get(split[0]);
            if(skill == null) return;
            if (!skill.isEnabled()) return;

            int level = Integer.parseInt(split[1]);
            if (level > skill.getMaxLevel()) level = skill.getMaxLevel();
            build.setSkill(type, skill, level);
            build.takePoints(level);
        }
    }

    @Override
    public void save(RoleBuild build) {
        String query = "INSERT IGNORE INTO " + tablePrefix + "builds VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        var swordStatement = new SkillStatementValue(build.getSwordSkill());
        var axeStatement = new SkillStatementValue(build.getAxeSkill());
        var bowStatement = new SkillStatementValue(build.getBow());
        var passiveAStatement = new SkillStatementValue(build.getPassiveA());
        var passiveBStatement = new SkillStatementValue(build.getPassiveB());
        var globalStatement = new SkillStatementValue(build.getGlobal());

        database.executeUpdateAsync(new Statement(query, new StringStatementValue(build.getUuid()), new StringStatementValue(build.getRole().getName()),
                new IntegerStatementValue(build.getId()),
                swordStatement, axeStatement, bowStatement,
                passiveAStatement, passiveBStatement, globalStatement,
                new BooleanStatementValue(build.isActive())));
    }

    public void update(RoleBuild build) {
        String query = "INSERT INTO " + tablePrefix + "builds VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE Sword = ?, Axe = ?, Bow = ?, PassiveA = ?, PassiveB = ?, Global = ?, Active = ?";

        var swordStatement = new SkillStatementValue(build.getSwordSkill());
        var axeStatement = new SkillStatementValue(build.getAxeSkill());
        var bowStatement = new SkillStatementValue(build.getBow());
        var passiveAStatement = new SkillStatementValue(build.getPassiveA());
        var passiveBStatement = new SkillStatementValue(build.getPassiveB());
        var globalStatement = new SkillStatementValue(build.getGlobal());

        database.executeUpdateAsync(new Statement(query, new StringStatementValue(build.getUuid()), new StringStatementValue(build.getRole().getName()),
                new IntegerStatementValue(build.getId()),
                swordStatement, axeStatement, bowStatement,
                passiveAStatement, passiveBStatement, globalStatement,
                new BooleanStatementValue(build.isActive()),
                swordStatement, axeStatement, bowStatement,
                passiveAStatement, passiveBStatement, globalStatement,
                new BooleanStatementValue(build.isActive())));
    }

    public void loadDefaultBuilds(GamerBuilds gamerBuilds) {

        String uuid = gamerBuilds.getUuid();

        List<RoleBuild> builds = new ArrayList<>();

        for (Role role : roleManager.getRoles()) {
            //load any new kits
            if (gamerBuilds.getBuild(role, 1).isEmpty()) {
                for (int d = 1; d < 5; d++) {
                    RoleBuild newClass = new RoleBuild(uuid, role, d);

                    newClass.setSkill(SkillType.SWORD, (Skill) role.getSwordSkills().stream().findFirst().orElseThrow(), 3);
                    newClass.setSkill(SkillType.AXE, (Skill) role.getAxeSkills().stream().findFirst().orElseThrow(), 5);
                    newClass.setSkill(SkillType.PASSIVE_A, (Skill) role.getPassiveA().stream().findFirst().orElseThrow(), 1);
                    newClass.setSkill(SkillType.PASSIVE_B, (Skill) role.getPassiveB().stream().findFirst().orElseThrow(), 3);
                    newClass.takePoints(12);

                    builds.add(newClass);
                }
            }
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
