package me.mykindos.betterpvp.champions.champions.builds;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;

@AllArgsConstructor
@Data
public class BuildSkill {

    private final Skill skill;
    private int level;

    public String getString() {

        return skill == null ? "" : skill.getName() + " " + getLevel();
    }

    public Component getComponent() {
        return getComponent(false);
    }

    public Component getComponent(boolean boosted) {
        if (skill == null) {
            return Component.empty();
        }
        int displayLevel = getLevel();
        if (boosted) {
            displayLevel++;
        }
        return UtilMessage.deserialize("<yellow>%s</yellow> (<green>%s</green>)", skill.getName(), displayLevel);
    }
}
