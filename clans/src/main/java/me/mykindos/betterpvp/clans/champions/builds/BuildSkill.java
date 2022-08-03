package me.mykindos.betterpvp.clans.champions.builds;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.clans.champions.skills.Skill;

@AllArgsConstructor
@Data
public class BuildSkill {

    private final Skill skill;
    private int level;

    public String getString() {
        return skill.getName() + " " + getLevel();
    }
}
