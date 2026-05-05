package me.mykindos.betterpvp.progression.profession.skill.tree;

import java.util.List;

public sealed interface SkillTreeCell permits SkillTreeCell.Skill, SkillTreeCell.Connection, SkillTreeCell.Air {

    record Skill(String skillId) implements SkillTreeCell {}

    /**
     * Visual connector tile. {@code linkedSkillIds} are the skill IDs at both ends of this
     * connection — used to determine active/inactive appearance in the GUI.
     */
    record Connection(ConnectionType type, List<String> linkedSkillIds) implements SkillTreeCell {}

    record Air() implements SkillTreeCell {}
}
