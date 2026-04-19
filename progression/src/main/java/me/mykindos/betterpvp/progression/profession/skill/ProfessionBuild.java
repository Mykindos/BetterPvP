package me.mykindos.betterpvp.progression.profession.skill;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProfessionBuild {

    private final String profession;
    private Map<ProfessionNode, Integer> skills;

    public ProfessionBuild(String profession) {
        this.profession = profession;
        this.skills = new HashMap<>();
    }

    public int getSkillLevel(ProfessionNode skill) {
        return skills.getOrDefault(skill, 0);
    }

}
