package me.mykindos.betterpvp.progression.profession.skill;

import lombok.Data;

import java.util.List;

@Data
public class ProfessionNodeDependency {

    private final List<String> nodes;
    private final int levelsRequired;
    private final int requiredLevel;

    public ProfessionNodeDependency(List<String> nodes, int levelsRequired, int requiredLevel) {
        this.nodes = nodes;
        this.levelsRequired = levelsRequired;
        this.requiredLevel = requiredLevel;
    }

    public ProfessionNodeDependency(List<String> dependencies, int levelsAssigned) {
        this(dependencies, levelsAssigned, 0);
    }



}
