package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.SkillNodeType;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class WoodcuttingProfessionMenu extends ProfessionMenu {
    public WoodcuttingProfessionMenu(WoodcuttingHandler woodcuttingHandler, ProfessionProfile professionProfile, ProfessionNodeManager progressionSkillManager) {
        super("Woodcutting", woodcuttingHandler, professionProfile, progressionSkillManager);

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public SkillNodeType getSkillNodeType() {
        return SkillNodeType.GREEN;
    }
}