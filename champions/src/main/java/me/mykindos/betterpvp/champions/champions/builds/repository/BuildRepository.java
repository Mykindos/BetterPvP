package me.mykindos.betterpvp.champions.champions.builds.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
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

@Singleton
public class BuildRepository implements IRepository<RoleBuild> {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
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

    public void loadBuilds(GamerBuilds builds) {
        String query = "SELECT * FROM " + tablePrefix + "builds WHERE Gamer = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(builds.getUuid())));
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

        if (!gamerBuilds.getBuilds().isEmpty()) return;

        String uuid = gamerBuilds.getUuid();

        List<RoleBuild> builds = new ArrayList<>();
        for (int d = 1; d < 5; d++) {

            RoleBuild assassin = new RoleBuild(uuid, Role.valueOf("ASSASSIN"), d);

            assassin.setSkill(SkillType.SWORD, skillManager.getObjects().get("Sever"), 3);
            assassin.setSkill(SkillType.AXE, skillManager.getObjects().get("Leap"), 5);
            assassin.setSkill(SkillType.PASSIVE_A, skillManager.getObjects().get("Backstab"), 1);
            assassin.setSkill(SkillType.PASSIVE_B, skillManager.getObjects().get("Smoke Bomb"), 3);
            assassin.takePoints(12);

            RoleBuild gladiator = new RoleBuild(uuid, Role.valueOf("GLADIATOR"), d);
            gladiator.setSkill(SkillType.SWORD, skillManager.getObjects().get("Takedown"), 5);
            gladiator.setSkill(SkillType.AXE, skillManager.getObjects().get("Seismic Slam"), 3);
            gladiator.setSkill(SkillType.PASSIVE_A, skillManager.getObjects().get("Colossus"), 1);
            gladiator.setSkill(SkillType.PASSIVE_B, skillManager.getObjects().get("Stampede"), 3);
            gladiator.takePoints(12);

            RoleBuild ranger = new RoleBuild(uuid, Role.valueOf("RANGER"), d);
            ranger.setSkill(SkillType.SWORD, skillManager.getObjects().get("Disengage"), 3);
            ranger.setSkill(SkillType.BOW, skillManager.getObjects().get("Incendiary Shot"), 5);
            ranger.setSkill(SkillType.PASSIVE_A, skillManager.getObjects().get("Longshot"), 3);
            ranger.setSkill(SkillType.PASSIVE_B, skillManager.getObjects().get("Sharpshooter"), 1);
            ranger.takePoints(12);

            RoleBuild paladin = new RoleBuild(uuid, Role.valueOf("PALADIN"), d);
            paladin.setSkill(SkillType.SWORD, skillManager.getObjects().get("Inferno"), 5);
            paladin.setSkill(SkillType.AXE, skillManager.getObjects().get("Molten Blast"), 3);
            paladin.setSkill(SkillType.PASSIVE_A, skillManager.getObjects().get("Holy Light"), 2);
            paladin.setSkill(SkillType.PASSIVE_B, skillManager.getObjects().get("Immolate"), 2);
            paladin.takePoints(12);

            RoleBuild knight = new RoleBuild(uuid, Role.valueOf("KNIGHT"), d);
            knight.setSkill(SkillType.SWORD, skillManager.getObjects().get("Riposte"), 3);
            knight.setSkill(SkillType.AXE, skillManager.getObjects().get("Bulls Charge"), 5);
            knight.setSkill(SkillType.PASSIVE_A, skillManager.getObjects().get("Fury"), 3);
            knight.setSkill(SkillType.PASSIVE_B, skillManager.getObjects().get("Swordsmanship"), 1);
            knight.takePoints(12);

            RoleBuild warlock = new RoleBuild(uuid, Role.valueOf("WARLOCK"), d);
            warlock.setSkill(SkillType.SWORD, skillManager.getObjects().get("Leech"), 4);
            warlock.setSkill(SkillType.AXE, skillManager.getObjects().get("Bloodshed"), 5);
            warlock.setSkill(SkillType.PASSIVE_A, skillManager.getObjects().get("Frailty"), 1);
            warlock.setSkill(SkillType.PASSIVE_B, skillManager.getObjects().get("Soul Harvest"), 2);
            warlock.takePoints(12);

            builds.addAll(List.of(knight, ranger, gladiator, paladin, assassin, warlock));

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
