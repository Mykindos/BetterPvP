package me.mykindos.betterpvp.progression.profession.skill.builds;

import lombok.Data;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProgressionBuild {

    private final String profession;
    private Map<ProgressionSkill, Integer> skills;

    public ProgressionBuild(String profession) {
        this.profession = profession;
        this.skills = new HashMap<>();
    }

    public int getSkillLevel(ProgressionSkill skill) {
        return skills.getOrDefault(skill, 0);
    }

}
