package me.mykindos.betterpvp.progression.profession.skill.menu;

import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.tree.NodeSlotType;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class MiningMenu extends ProfessionMenu {

    public MiningMenu(MiningHandler miningHandler, ProfessionProfile professionProfile, ProfessionNodeManager professionNodeManager) {
        super("Mining", miningHandler, professionProfile, professionNodeManager);

       //// Add skills to the menu
       //progressionSkillManager.getSkill("Smelter").ifPresent(this::addSkill);
       //progressionSkillManager.getSkill("Gold Rush").ifPresent(this::addSkill);
       //progressionSkillManager.getSkill("Vein Vindicator").ifPresent(this::addSkill);

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public NodeSlotType getNodeSlotType() {
        return NodeSlotType.RED;
    }
}
