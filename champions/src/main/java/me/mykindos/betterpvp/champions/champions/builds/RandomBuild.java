package me.mykindos.betterpvp.champions.champions.builds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.entity.Player;

import java.util.List;

//Generate a random build
@Singleton
public class RandomBuild {

    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;

    @Inject
    public RandomBuild(BuildManager buildManager, ChampionsSkillManager championsSkillManager) {
        this.buildManager = buildManager;
        this.championsSkillManager = championsSkillManager;
    }


    /**
     * Generates a random build for the specified role
     * This is a destructive option, it replaces the build for the specified Role/ID
     * @param player the player generating the build
     * @param role the role to generate the build for
     * @param id the id of the build to generate the build for
     */
    public RoleBuild generateRandomBuild(Player player, Role role, int id) {
        //First, generate a set of valid skills
        List<Skill> elligibleSkills = new java.util.ArrayList<>(championsSkillManager.getSkillsForRole(role).stream().filter(Skill::isEnabled).toList());

        //player should already have a valid build
        RoleBuild build = buildManager.getObject(player.getUniqueId()).orElseThrow().getBuild(role, id).orElseThrow();
        //this is a destructive option, delete the current build
        build.deleteBuild();
        //while we still have tokens, get a new skill

        for (int i = build.getPoints(); i > 0; i--) {
            //choose an eligible skill
            Skill skill = elligibleSkills.get(UtilMath.randomInt(0, elligibleSkills.size()));
            if (build.getActiveSkills().contains(skill)) {
                //we have this skill already, need to update it
                BuildSkill buildSkill = build.getBuildSkill(skill.getType());
                buildSkill.setLevel(buildSkill.getLevel() + 1);
                if (buildSkill.getLevel() == buildSkill.getSkill().getMaxLevel()) {
                    //we cannot put more levels in this skill, it is now ineligible
                    elligibleSkills.remove(skill);
                }
            } else {
                //this is a new skill
                build.setSkill(skill.getType(), skill, 1);
                //we now need to invalidate all other skills of this type
                elligibleSkills.removeIf(elligibleSkill -> {
                    if (!skill.equals(elligibleSkill)) {
                        return skill.getType() == elligibleSkill.getType();
                    }
                    return false;
                });
            }
            build.takePoint();
        }
        //now, we need to update the build
        buildManager.getBuildRepository().update(build);
        return build;
    }
}
