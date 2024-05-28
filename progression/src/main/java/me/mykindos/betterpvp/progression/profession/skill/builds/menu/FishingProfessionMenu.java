package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProgressionSkillButton;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class FishingProfessionMenu extends ProfessionMenu {

    public FishingProfessionMenu(ProfessionProfile professionProfile, ProgressionSkillManager progressionSkillManager) {
        super("Fishing", professionProfile, progressionSkillManager);

        progressionSkillManager.getSkill("Thicker Lines").ifPresent(skill -> {
            setItem(11, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Feeling Lucky").ifPresent(skill -> {
            setItem(13, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Expert Baiter").ifPresent(skill -> {
            setItem(15, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Base Fishing").ifPresent(skill -> {
            setItem(30, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("No More Mobs").ifPresent(skill -> {
            setItem(32, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Swiftness").ifPresent(skill -> {
            setItem(49, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
    }

}
