package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree.SkillNodeType;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class FishingProfessionMenu extends ProfessionMenu {

    public FishingProfessionMenu(FishingHandler fishingHandler, ProfessionProfile professionProfile, ProfessionNodeManager progressionSkillManager) {
        super("Fishing", fishingHandler, professionProfile, progressionSkillManager);

        // Update the content to display the skills
        updateContent();
    }

    @Override
    public SkillNodeType getSkillNodeType() {
        return SkillNodeType.BLUE;
    }
}