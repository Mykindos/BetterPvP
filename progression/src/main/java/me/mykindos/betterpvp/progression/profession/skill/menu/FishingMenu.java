package me.mykindos.betterpvp.progression.profession.skill.menu;

import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.tree.NodeSlotType;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class FishingMenu extends ProfessionMenu {

    public FishingMenu(FishingHandler fishingHandler, ProfessionProfile professionProfile, ProfessionNodeManager progressionSkillManager) {
        super("Fishing", fishingHandler, professionProfile, progressionSkillManager);

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public NodeSlotType getNodeSlotType() {
        return NodeSlotType.BLUE;
    }
}