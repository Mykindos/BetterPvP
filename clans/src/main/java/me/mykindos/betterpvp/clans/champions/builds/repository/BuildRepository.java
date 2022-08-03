package me.mykindos.betterpvp.clans.champions.builds.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.gamer.Gamer;
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

@Singleton
public class BuildRepository implements IRepository<RoleBuild> {

    @Inject
    @Config(path = "clans.database.prefix")
    private String tablePrefix;

    private final Database database;
    private final SkillManager skillManager;

    @Inject
    public BuildRepository(Database database, SkillManager skillManager) {
        this.database = database;
        this.skillManager = skillManager;
    }

    @Override
    public List<RoleBuild> getAll() {

        return null;
    }

    public void loadBuilds(Gamer gamer) {
        String query = "SELECT * FROM " + tablePrefix + "champions_builds WHERE Gamer = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(gamer.getUuid())));
        List<RoleBuild> builds = new ArrayList<>();
        try {
            while (result.next()) {
                String uuid = result.getString(1);
                String role = result.getString(2);
                int id = result.getInt(3);
                RoleBuild build = new RoleBuild(uuid, Role.valueOf(role.toUpperCase()), id);

                String sword = result.getString(4);
                setSkill(build, SkillType.SWORD, sword);

                String axe = result.getString(5);
                setSkill(build, SkillType.AXE, axe);

                String bow = result.getString(6);
                setSkill(build, SkillType.BOW, bow);

                String passiveA = result.getString(7);
                setSkill(build, SkillType.PASSIVE_A, passiveA);

                String passiveB = result.getString(8);
                setSkill(build, SkillType.PASSIVE_B, passiveB);

                String global = result.getString(9);
                setSkill(build, SkillType.GLOBAL, global);

                boolean active = result.getBoolean(10);
                build.setActive(active);

                if (active) {
                    gamer.getActiveBuilds().put(role, build);
                }

                gamer.getBuilds().add(build);

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private void setSkill(RoleBuild build, SkillType type, String value) {

        if (value != null) {
            String[] split = value.split(",");
            Optional<Skill> skillOptional = skillManager.getObject(split[0]);
            if (skillOptional.isPresent()) {
                Skill skill = skillOptional.get();
                int level = Integer.parseInt(split[1]);
                build.setSkill(type, skill, level);
                build.takePoints(level);

            }
        }
    }

    @Override
    public void save(RoleBuild build) {
        String query = "INSERT INTO " + tablePrefix + "champions_builds VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
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
}
