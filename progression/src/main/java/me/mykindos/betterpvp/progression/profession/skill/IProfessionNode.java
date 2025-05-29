package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.skill.ISkill;

import javax.annotation.Nullable;

public interface IProfessionNode extends ISkill {

    default @Nullable ProfessionNodeDependency getDependencies() {
        return null;
    }

    String getProgressionTree();

}
