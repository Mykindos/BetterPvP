package me.mykindos.betterpvp.progression.profession.skill;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProfessionBuild {

    private final String profession;
    private Map<ProfessionNode, Integer> nodes;

    public ProfessionBuild(String profession) {
        this.profession = profession;
        this.nodes = new HashMap<>();
    }

    public int getSkillLevel(ProfessionNode skill) {
        return nodes.getOrDefault(skill, 0);
    }

}
