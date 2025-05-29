package me.mykindos.betterpvp.progression.profession.skill.builds.menu.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SkillNodeType {

    BLUE(421, 424),
    RED(420, 423),
    GREEN(419, 422),
    YELLOW(415, 418);

    private final int startedModelData;
    private final int completedModelData;
}
