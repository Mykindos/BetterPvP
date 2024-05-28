package me.mykindos.betterpvp.progression.profession.skill;

import lombok.Data;

@Data
public class ProgressionSkillDependency {

    private final String[] dependencies;
    private final int levelsAssigned;

}
