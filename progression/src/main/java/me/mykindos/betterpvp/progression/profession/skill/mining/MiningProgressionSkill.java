package me.mykindos.betterpvp.progression.profession.skill.mining;

import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

public abstract class MiningProgressionSkill extends ProgressionSkill {

    protected MiningProgressionSkill(Progression progression) {
        super(progression);
    }

    @Override
    public String getProgressionTree() {
        return "Mining";
    }

    protected int getPlayerSkillLevel(ProfessionProfile profile) {
        var profession = profile.getProfessionDataMap().get("Mining");
        if (profession == null) return 0;

        return profession.getBuild().getSkillLevel(this);
    }
}
