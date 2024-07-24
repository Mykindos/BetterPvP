package me.mykindos.betterpvp.champions.champions.builds.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.UUID.fromString;

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
        String query = "SELECT * FROM champions_builds WHERE Gamer = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(builds.getUuid())));
        try {
            while (result.next()) {
                String uuid = result.getString(1);
                String role = result.getString(2);
                int id = result.getInt(3);
                RoleBuild build = new RoleBuild(uuid, Role.valueOf(role.toUpperCase()), id);

                boolean active = result.getBoolean(10);
                build.setActive(active);

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

                if (active) {
                    builds.getActiveBuilds().put(role, build);
                }

                builds.getBuilds().add(build);

            }
        } catch (SQLException ex) {
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
            Player player = Bukkit.getPlayer(fromString(build.getUuid()));
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
        String query = "INSERT IGNORE INTO champions_builds VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
        String query = "INSERT INTO champions_builds VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
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
            setSkill(assassin, SkillType.SWORD, "Sever", 3);
            setSkill(assassin, SkillType.AXE, "Leap", 5);
            setSkill(assassin, SkillType.PASSIVE_A, "Smoke Bomb", 3);
            setSkill(assassin, SkillType.PASSIVE_B, "Backstab", 1);

            RoleBuild brute = new RoleBuild(uuid, Role.valueOf("BRUTE"), d);
            setSkill(brute, SkillType.SWORD, "Flesh Hook", 3);
            setSkill(brute, SkillType.AXE, "Seismic Slam", 5);
            setSkill(brute, SkillType.PASSIVE_A, "Stampede", 3);
            setSkill(brute, SkillType.PASSIVE_B, "Colossus", 1);

            RoleBuild ranger = new RoleBuild(uuid, Role.valueOf("RANGER"), d);
            setSkill(ranger, SkillType.SWORD, "Disengage", 3);
            setSkill(ranger, SkillType.BOW, "Incendiary Shot", 5);
            setSkill(ranger, SkillType.PASSIVE_B, "Longshot", 3);
            setSkill(ranger, SkillType.PASSIVE_A, "Barbed Arrows", 1);

            RoleBuild mage = new RoleBuild(uuid, Role.valueOf("MAGE"), d);
            setSkill(mage, SkillType.SWORD, "Inferno", 5);
            setSkill(mage, SkillType.AXE, "Fire Blast", 3);
            setSkill(mage, SkillType.PASSIVE_A, "Immolate", 2);
            setSkill(mage, SkillType.PASSIVE_B, "Holy Light", 2);

            RoleBuild knight = new RoleBuild(uuid, Role.valueOf("KNIGHT"), d);
            setSkill(knight, SkillType.SWORD, "Riposte", 3);
            setSkill(knight, SkillType.AXE, "Bulls Charge", 5);
            setSkill(knight, SkillType.PASSIVE_A, "Swordsmanship", 1);
            setSkill(knight, SkillType.PASSIVE_B, "Vengeance", 3);

            RoleBuild warlock = new RoleBuild(uuid, Role.valueOf("WARLOCK"), d);
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
