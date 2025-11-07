package me.mykindos.betterpvp.champions.champions.builds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.event.ChampionsBuildLoadedEvent;
import me.mykindos.betterpvp.champions.champions.builds.repository.BuildRepository;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
@Getter
public class BuildManager extends Manager<String, GamerBuilds> {

    private final ClientManager clientManager;
    private final BuildRepository buildRepository;
    private final Champions champions;
    private final ChampionsSkillManager championsSkillManager;

    @Inject
    public BuildManager(ClientManager clientManager, BuildRepository buildRepository, Champions champions, ChampionsSkillManager championsSkillManager) {
        this.clientManager = clientManager;
        this.buildRepository = buildRepository;
        this.champions = champions;
        this.championsSkillManager = championsSkillManager;
    }

    public void loadBuilds(Player player) {
        GamerBuilds builds = new GamerBuilds(clientManager.search().online(player));
        getBuildRepository().loadBuilds(builds);
        getBuildRepository().loadDefaultBuilds(builds);
        addObject(player.getUniqueId().toString(), builds);
        UtilServer.runTask(champions, () -> {
            UtilServer.callEvent(new ChampionsBuildLoadedEvent(player, builds));
        });
    }

    /**
     * gets a random build for the player, for the specific role and build id
     * <p>Does not override current role build.</p>
     * @param player the player
     * @param role the role
     * @param id the id of the build
     * @return the Random RoleBuild
     */
    public RoleBuild getRandomBuild(Player player, Role role, int id) {
        //First, generate a set of valid skills
        List<Skill> eligibleSkills = new java.util.ArrayList<>(championsSkillManager.getSkillsForRole(role).stream().filter(Skill::isEnabled).toList());

        Client client = clientManager.search().online(player);
        //player should already have a valid build
        RoleBuild build = new RoleBuild(client.getId(), player.getUniqueId(), role, id);
        for (int i = build.getPoints(); i > 0; i--) {
            //choose an eligible skill
            Skill skill = eligibleSkills.get(UtilMath.randomInt(0, eligibleSkills.size()));
            if (build.getActiveSkills().contains(skill)) {
                //we have this skill already, need to update it
                BuildSkill buildSkill = build.getBuildSkill(skill.getType());
                buildSkill.setLevel(buildSkill.getLevel() + 1);
                if (buildSkill.getLevel() == buildSkill.getSkill().getMaxLevel()) {
                    //we cannot put more levels in this skill, it is now ineligible
                    eligibleSkills.remove(skill);
                }
            } else {
                //this is a new skill
                build.setSkill(skill.getType(), skill, 1);
                //remove this skill if it is a single level skill
                if (skill.getMaxLevel() == 1) {
                    eligibleSkills.remove(skill);
                }
                //we now need to invalidate all other skills of this type
                eligibleSkills.removeIf(elligibleSkill -> {
                    if (!skill.equals(elligibleSkill)) {
                        return skill.getType() == elligibleSkill.getType();
                    }
                    return false;
                });
            }
            build.takePoint();
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
        RoleBuild newRoleBuild = getRandomBuild(player, role, id);
        this.getObject(player.getUniqueId().toString()).orElseThrow().setBuild(newRoleBuild, role, id);
        getBuildRepository().update(newRoleBuild);
        return newRoleBuild;
    }

    public void reloadBuilds() {
        getObjects().clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadBuilds(player);
        }
    }


}
