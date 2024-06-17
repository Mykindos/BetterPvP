package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;

/**
 * Base class for a Woodcutting Progression Skill
 */
public abstract class WoodcuttingProgressionSkill extends ProgressionSkill {
    protected WoodcuttingProgressionSkill(Progression progression) {
        super(progression);
    }

    /**
     * This method is used to more so just get the name of these types of skills;
     * Yet, it's name makes it sound like your getting back some tree struct,
     * but it's just a String
     */
    @Override
    public String getProgressionTree() {
        return "Woodcutting";
    }
}
