package me.mykindos.betterpvp.champions.champions.builds;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.Skill;

@AllArgsConstructor
@Data
public class BuildSkill {

    private final Skill skill;
    private int level;

    public String getString() {

        return skill == null ? "" : skill.getName() + " " + getLevel();
    }
}
