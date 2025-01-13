package me.mykindos.betterpvp.champions.champions.builds;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.event.ChampionsBuildLoadedEvent;
import me.mykindos.betterpvp.champions.champions.builds.repository.BuildRepository;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
@Getter
public class BuildManager extends Manager<GamerBuilds> {

    private final BuildRepository buildRepository;
    private final Champions champions;
    private final ChampionsSkillManager championsSkillManager;

    @Inject
    public BuildManager(BuildRepository buildRepository, Champions champions, ChampionsSkillManager championsSkillManager) {
        this.buildRepository = buildRepository;
        this.champions = champions;
        this.championsSkillManager = championsSkillManager;
    }

    public void loadBuilds(Player player) {
        GamerBuilds builds = new GamerBuilds(player.getUniqueId().toString());
        getBuildRepository().loadBuilds(builds);
        getBuildRepository().loadDefaultBuilds(builds);
        addObject(player.getUniqueId().toString(), builds);
        UtilServer.runTask(champions, () -> {
            UtilServer.callEvent(new ChampionsBuildLoadedEvent(player, builds));
        });
    }

    public RoleBuild getRandomBuild(Player player, Role role, int id) {
        //First, generate a set of valid skills
        ArrayListMultimap<SkillType, Skill> skills = ArrayListMultimap.create();
        championsSkillManager.getSkillsForRole(role)
                .stream()
                .filter(Skill::isEnabled)
                .forEach(skill -> skills.put(skill.getType(), skill));

        RoleBuild build = new RoleBuild(player.getUniqueId().toString(), role, id);
        for (SkillType type : SkillType.values()) {
            final List<Skill> pool = skills.get(type);
            Skill skill = pool.get(UtilMath.randomInt(0, pool.size()));
            build.setSkill(type, skill);
        }

        return build;
    }

    /**
     * Generates a random build for the specified role
     * This is a destructive option, it replaces the build for the specified Role/ID
     * @param player the player generating the build
     * @param role the role to generate the build for
     * @param id the id of the build to generate the build for
     */
    public RoleBuild generateRandomBuild(Player player, Role role, int id) {
        RoleBuild newRoleBuld = generateRandomBuild(player, role, id);
        this.getObject(player.getUniqueId()).orElseThrow().setBuild(newRoleBuld, role, id);
        getBuildRepository().update(newRoleBuld);
        return newRoleBuld;
    }

    public void reloadBuilds() {
        getObjects().clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadBuilds(player);
        }
    }
}
