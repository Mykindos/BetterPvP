package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.skill.ISkill;

import javax.annotation.Nullable;

public interface IProgressionSkill extends ISkill {

    default @Nullable ProgressionSkillDependency getDependencies() {
        return null;
    }

    String getProgressionTree();

}
