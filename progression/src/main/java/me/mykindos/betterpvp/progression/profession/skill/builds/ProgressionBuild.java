package me.mykindos.betterpvp.progression.profession.skill.builds;

import lombok.Data;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProgressionBuild {

    private final String profession;
    private Map<ProfessionNode, Integer> skills;

    public ProgressionBuild(String profession) {
        this.profession = profession;
        this.skills = new HashMap<>();
    }

    public int getSkillLevel(ProfessionNode skill) {
        return skills.getOrDefault(skill, 0);
    }

}
