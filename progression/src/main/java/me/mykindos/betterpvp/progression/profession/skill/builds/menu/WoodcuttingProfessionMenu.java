package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProgressionSkillButton;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class WoodcuttingProfessionMenu extends ProfessionMenu {
    public WoodcuttingProfessionMenu(ProfessionProfile professionProfile, ProgressionSkillManager progressionSkillManager) {
        super("Woodcutting", professionProfile, progressionSkillManager);

        // Start Tier 1
        progressionSkillManager.getSkill("Bark Bounty").ifPresent(skill -> {
            setItem(11, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Tree Tactician").ifPresent(skill -> {
            setItem(13, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Forest Flourisher").ifPresent(skill -> {
            setItem(15, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
        // End Tier 1

        // Start Tier 2
        progressionSkillManager.getSkill("Auto Planter").ifPresent(skill -> {
            setItem(29, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Tree Feller").ifPresent(skill -> {
            setItem(31, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Tree Compactor").ifPresent(skill -> {
            setItem(33, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
        // End Tier 2

        // Start Tier 3
        progressionSkillManager.getSkill("Enchanted Lumberfall").ifPresent(skill -> {
            setItem(49, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
        // End Tier 3




    }
}
