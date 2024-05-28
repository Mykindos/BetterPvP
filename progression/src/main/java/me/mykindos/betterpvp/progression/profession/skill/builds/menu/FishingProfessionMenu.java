package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProgressionSkillButton;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class FishingProfessionMenu extends ProfessionMenu {

    public FishingProfessionMenu(ProfessionProfile professionProfile, ProgressionSkillManager progressionSkillManager) {
        super("Fishing", professionProfile, progressionSkillManager);

        progressionSkillManager.getSkill("Thicker Lines").ifPresent(skill -> {
            setItem(10, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Feeling Lucky").ifPresent(skill -> {
            setItem(28, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Expert Baiter").ifPresent(skill -> {
            setItem(46, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Base Fishing").ifPresent(skill -> {
            setItem(22, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("No More Mobs").ifPresent(skill -> {
            setItem(40, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Swiftness").ifPresent(skill -> {
            setItem(34, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
    }

}
