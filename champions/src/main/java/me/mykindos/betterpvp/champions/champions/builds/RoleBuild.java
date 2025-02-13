package me.mykindos.betterpvp.champions.champions.builds;

import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class RoleBuild {

    private final String uuid;
    private final Role role;
    private final int id;

    private boolean active;
    private Skill sword;
    private Skill axe;
    private Skill passiveA;
    private Skill passiveB;
    private Skill global;
    private Skill bow;

    public Collection<Skill> getActiveSkills() {
        ArrayList<Skill> skills = new ArrayList<>();
        if (sword != null) skills.add(sword);
        if (axe != null) skills.add(axe);
        if (passiveA != null) skills.add(passiveA);
        if (passiveB != null) skills.add(passiveB);
        if (global != null) skills.add(global);
        if (bow != null) skills.add(bow);
        return skills;
    }

    public Skill getSkill(SkillType type) {
        return switch (type) {
            case SWORD -> sword;
            case AXE -> axe;
            case PASSIVE_A -> passiveA;
            case BOW -> bow;
            case GLOBAL -> global;
            case PASSIVE_B -> passiveB;
        };
    }

    public void setSkill(SkillType type, Skill skill) {
        switch (type) {
            case SWORD -> setSword(skill);
            case AXE -> setAxe(skill);
            case PASSIVE_A -> setPassiveA(skill);
            case BOW -> setBow(skill);
            case GLOBAL -> setGlobal(skill);
            case PASSIVE_B -> setPassiveB(skill);
        }
    }

    public void resetBuild() {
        sword = null;
        axe = null;
        passiveA = null;
        passiveB = null;
        global = null;
        bow = null;
    }

    private Component toComponent(SkillType type) {
        Skill skill = switch (type) {
            case SWORD -> sword;
            case AXE -> axe;
            case PASSIVE_A -> passiveA;
            case BOW -> bow;
            case GLOBAL -> global;
            case PASSIVE_B -> passiveB;
        };

        if (skill == null) {
            return Component.text("None", NamedTextColor.RED);
        }

        return skill.toComponent();
    }

    /**
     * @return The component representation of a build
     */
    public Component getBuildComponent() {
        Component sword = toComponent(SkillType.SWORD);
        Component axe = toComponent(SkillType.AXE);
        Component bow = toComponent(SkillType.BOW);
        Component passiveA = toComponent(SkillType.PASSIVE_A);
        Component passiveB = toComponent(SkillType.PASSIVE_B);
        Component global = toComponent(SkillType.GLOBAL);

        return Component.text("Sword: ", NamedTextColor.WHITE).append(sword).appendNewline()
                .append(Component.text("Axe: ", NamedTextColor.WHITE).append(axe).appendNewline())
                .append(Component.text("Bow: ", NamedTextColor.WHITE).append(bow).appendNewline())
                .append(Component.text("Passive A: ", NamedTextColor.WHITE).append(passiveA).appendNewline())
                .append(Component.text("Passive B: ", NamedTextColor.WHITE).append(passiveB).appendNewline())
                .append(Component.text("Global: ", NamedTextColor.WHITE).append(global));
    }

    /**
     * Returns true if the role and BuildSkills are all equal
     * @param o the object to check against
     * @return true if this object has the same build
     */
    public boolean buildEquals(Object o) {
        if (super.equals(o)) {
            return true;
        }
        if (!(o instanceof RoleBuild r2)) {
            return false;
        }

        if (this.role != r2.getRole()) {
            return false;
        }

        for (SkillType skillType : SkillType.values()) {
            Skill first = getSkill(skillType);
            Skill second = r2.getSkill(skillType);

            if (first == null && second != null) {
                return false;
            }
            // still must check for null, because 2 null skills are valid
            if (first != null && (!first.equals(second))) {
                return false;
            }
        }
        return true;
    }
}
