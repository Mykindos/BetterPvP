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

    private void setSkill(RoleBuild build, SkillType type, String skillName) {
        Skill skill = skillManager.getObjects().get(skillName);
        if (skill == null) return;
        if (!skill.isEnabled()) {
            if (!build.isActive()) return;
            Player player = Bukkit.getPlayer(fromString(build.getUuid()));
            if (player == null) return;
            UtilMessage.message(player, "Champions", UtilMessage.deserialize("<green>%s</green> has been disabled on this server, removing from <yellow>%s</yellow> build <green>%s</green>", skill.getName(), build.getRole().toString(), build.getId()));
            return;
        }

        if (skill.getType() != type) {
            return;
        }

        build.setSkill(type, skill);
    }

    @Override
    public void save(RoleBuild build) {
        String query = "INSERT IGNORE INTO champions_builds VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        var swordStatement = new SkillStatementValue(build.getSword());
        var axeStatement = new SkillStatementValue(build.getAxe());
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

        var swordStatement = new SkillStatementValue(build.getSword());
        var axeStatement = new SkillStatementValue(build.getAxe());
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
            setSkill(assassin, SkillType.SWORD, "Sever");
            setSkill(assassin, SkillType.AXE, "Leap");
            setSkill(assassin, SkillType.BOW, "Smoke Arrow");
            setSkill(assassin, SkillType.PASSIVE_A, "Smoke Bomb");
            setSkill(assassin, SkillType.PASSIVE_B, "Backstab");
            setSkill(assassin, SkillType.GLOBAL, "Tranquility");

            RoleBuild brute = new RoleBuild(uuid, Role.valueOf("BRUTE"), d);
            setSkill(brute, SkillType.SWORD, "Flesh Hook");
            setSkill(brute, SkillType.AXE, "Seismic Slam");
            setSkill(brute, SkillType.PASSIVE_A, "Stampede");
            setSkill(brute, SkillType.PASSIVE_B, "Overwhelm");
            setSkill(brute, SkillType.GLOBAL, "Break Fall");

            RoleBuild ranger = new RoleBuild(uuid, Role.valueOf("RANGER"), d);
            setSkill(ranger, SkillType.SWORD, "Disengage");
            setSkill(ranger, SkillType.AXE, "Agility");
            setSkill(ranger, SkillType.BOW, "Napalm Arrow");
            setSkill(ranger, SkillType.PASSIVE_A, "Hunters Thrill");
            setSkill(ranger, SkillType.PASSIVE_B, "Kinetics");
            setSkill(ranger, SkillType.GLOBAL, "Resistance");

            RoleBuild mage = new RoleBuild(uuid, Role.valueOf("MAGE"), d);
            setSkill(mage, SkillType.SWORD, "Inferno");
            setSkill(mage, SkillType.AXE, "Fire Blast");
            setSkill(mage, SkillType.PASSIVE_A, "Immolate");
            setSkill(mage, SkillType.PASSIVE_B, "Magma Blade");
            setSkill(mage, SkillType.GLOBAL, "Fast Recovery");

            RoleBuild knight = new RoleBuild(uuid, Role.valueOf("KNIGHT"), d);
            setSkill(knight, SkillType.SWORD, "Riposte");
            setSkill(knight, SkillType.AXE, "Bulls Charge");
            setSkill(knight, SkillType.PASSIVE_A, "Swordsmanship");
            setSkill(knight, SkillType.PASSIVE_B, "Vengeance");
            setSkill(knight, SkillType.GLOBAL, "Level Field");

            RoleBuild warlock = new RoleBuild(uuid, Role.valueOf("WARLOCK"), d);
            setSkill(warlock, SkillType.SWORD, "Leech");
            setSkill(warlock, SkillType.AXE, "Clone");
            setSkill(warlock, SkillType.PASSIVE_A, "Impotence");
            setSkill(warlock, SkillType.PASSIVE_B, "Siphon");
            setSkill(warlock, SkillType.GLOBAL, "Level Field");

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
