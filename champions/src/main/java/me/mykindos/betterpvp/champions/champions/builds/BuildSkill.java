package me.mykindos.betterpvp.champions.champions.builds;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
        return Component.empty()
                .append(skill.getDisplayName().color(NamedTextColor.YELLOW))
                .append(Component.text(" ("))
                .append(Component.text(displayLevel, NamedTextColor.GREEN))
                .append(Component.text(")"));
    }

    public BuildSkill copy() {
        return new BuildSkill(getSkill(), getLevel());
    }
}
