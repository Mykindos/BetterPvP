package me.mykindos.betterpvp.progression.profession.skill.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.tree.NodeSlotType;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class WoodcuttingMenu extends ProfessionMenu {
    public WoodcuttingMenu(WoodcuttingHandler woodcuttingHandler, ProfessionProfile professionProfile, ProfessionNodeManager progressionSkillManager) {
        super("Woodcutting", woodcuttingHandler, professionProfile, progressionSkillManager);

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public NodeSlotType getNodeSlotType() {
        return NodeSlotType.GREEN;
    }
}