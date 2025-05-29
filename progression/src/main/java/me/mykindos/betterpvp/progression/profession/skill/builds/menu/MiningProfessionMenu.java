package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.SkillNodeType;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class MiningProfessionMenu extends ProfessionMenu {

    public MiningProfessionMenu(MiningHandler miningHandler, ProfessionProfile professionProfile, ProfessionNodeManager professionNodeManager) {
        super("Mining", miningHandler, professionProfile, professionNodeManager);

       //// Add skills to the menu
       //progressionSkillManager.getSkill("Smelter").ifPresent(this::addSkill);
       //progressionSkillManager.getSkill("Gold Rush").ifPresent(this::addSkill);
       //progressionSkillManager.getSkill("Vein Vindicator").ifPresent(this::addSkill);

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public SkillNodeType getSkillNodeType() {
        return SkillNodeType.RED;
    }
}
