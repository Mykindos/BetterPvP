package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProgressionSkillButton;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class WoodcuttingProfessionMenu extends ProfessionMenu {
    public WoodcuttingProfessionMenu(ProfessionProfile professionProfile, ProgressionSkillManager progressionSkillManager) {
        super("Woodcutting", professionProfile, progressionSkillManager);

        progressionSkillManager.getSkill("Tree Tactician").ifPresent(skill -> {
            setItem(13, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Tree Feller").ifPresent(skill -> {
            setItem(31, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Forest Flourisher").ifPresent(skill -> {
            setItem(38, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Bark Bounty").ifPresent(skill -> {
            setItem(40, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Enchanted Lumberfall").ifPresent(skill -> {
            setItem(42, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Auto Planter").ifPresent(skill -> {
            setItem(47, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
    }
}
