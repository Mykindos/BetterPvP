package me.mykindos.betterpvp.progression.profession.skill.fishing;

import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;

public abstract class FishingProgressionSkill extends ProgressionSkill {

    protected FishingProgressionSkill(Progression progression) {
        super(progression);
    }

    @Override
    public String getProgressionTree() {
        return "Fishing";
    }

}
