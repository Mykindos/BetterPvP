package me.mykindos.betterpvp.progression.profession.skill.builds.menu;

import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.menu.buttons.ProgressionSkillButton;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public class MiningProfessionMenu extends ProfessionMenu {

    public MiningProfessionMenu(ProfessionProfile professionProfile, ProgressionSkillManager progressionSkillManager) {
        super("Mining", professionProfile, progressionSkillManager);

        progressionSkillManager.getSkill("Smelter").ifPresent(skill -> {
            setItem(12, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });

        progressionSkillManager.getSkill("Gold Rush").ifPresent(skill -> {
            setItem(14, new ProgressionSkillButton(skill, professionData, progressionSkillManager));
        });
    }

}
