package me.mykindos.betterpvp.champions.champions.builds;

import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;

@Data
public class RoleBuild {

    private final String uuid;
    private final Role role;
    private final int id;

    private boolean active;
    private BuildSkill swordSkill;
    private BuildSkill axeSkill;
    private BuildSkill passiveA, passiveB, global;
    private BuildSkill bow;
    private int points = 12;

    public void addPoint() {
        points++;
    }

    public void takePoint() {
        points--;
    }

    public void takePoints(int points) {
        this.points -= points;
    }

    public ArrayList<Skill> getActiveSkills() {
        ArrayList<Skill> skills = new ArrayList<>();
        if (swordSkill != null) {
            skills.add(swordSkill.getSkill());
        }
        if (axeSkill != null) {
            skills.add(axeSkill.getSkill());
        }
        if (getBow() != null) {
            skills.add(getBow().getSkill());
        }
        if (getPassiveA() != null) {
            skills.add(getPassiveA().getSkill());
        }
        if (getPassiveB() != null) {
            skills.add(getPassiveB().getSkill());
        }
        if (getGlobal() != null) {
            skills.add(getGlobal().getSkill());
        }
        return skills;
    }

    public BuildSkill getBuildSkill(SkillType type) {
        return switch (type) {
            case SWORD -> getSwordSkill();
            case AXE -> getAxeSkill();
            case PASSIVE_A -> getPassiveA();
            case BOW -> getBow();
            case GLOBAL -> getGlobal();
            case PASSIVE_B -> getPassiveB();
        };

    }


    public void setSkill(SkillType type, BuildSkill skill) {
        switch (type) {
            case SWORD -> setSwordSkill(skill);
            case AXE -> setAxeSkill(skill);
            case PASSIVE_A -> setPassiveA(skill);
            case BOW -> setBow(skill);
            case GLOBAL -> setGlobal(skill);
            case PASSIVE_B -> setPassiveB(skill);
        }
    }

    public void setSkill(SkillType type, Skill skill, int level) {
        setSkill(type, new BuildSkill(skill, level));
    }

    public void deleteBuild() {
        swordSkill = null;
        axeSkill = null;
        passiveA = null;
        passiveB = null;
        global = null;
        bow = null;
        points = 12;

    }

    public Component getBuildSkillComponent(SkillType type) {
        Skill skill;
        BuildSkill buildSkill = null;
        switch (type) {
            case SWORD -> buildSkill = swordSkill;
            case AXE -> buildSkill = axeSkill;
            case PASSIVE_A -> buildSkill = passiveA;
            case BOW -> buildSkill = bow;
            case GLOBAL -> buildSkill = global;
            case PASSIVE_B -> buildSkill = passiveB;
        }
        if (buildSkill == null || buildSkill.getSkill() == null) {
            return Component.empty();
        }
        skill = buildSkill.getSkill();
        Component descriptionComponent = UtilMessage.deserialize("<yellow>%s</yellow> (<green>%s</green>)", skill.getName(), buildSkill.getLevel());

        for (Component component : skill.parseDescription(buildSkill.getLevel())) {
            descriptionComponent = descriptionComponent.appendNewline().append(component);
        }
        return buildSkill.getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + skill.getName().replace(" ", "_") + " " + buildSkill.getLevel()))
                .hoverEvent(HoverEvent.showText(descriptionComponent));
    }

    /**
     * @return The component representation of a build
     */
    public Component getBuildComponent() {
        Component sword = getBuildSkillComponent(SkillType.SWORD);
        Component axe = getBuildSkillComponent(SkillType.AXE);
        Component bow = getBuildSkillComponent(SkillType.BOW);
        Component passivea = getBuildSkillComponent(SkillType.PASSIVE_A);
        Component passiveb = getBuildSkillComponent(SkillType.PASSIVE_B);
        Component global = getBuildSkillComponent(SkillType.GLOBAL);

        Component component = Component.text("Sword: ", NamedTextColor.WHITE).append(sword).appendNewline()
                .append(Component.text("Axe: ", NamedTextColor.WHITE).append(axe).appendNewline())
                .append(Component.text("Bow: ", NamedTextColor.WHITE).append(bow).appendNewline())
                .append(Component.text("Passive A: ", NamedTextColor.WHITE).append(passivea).appendNewline())
                .append(Component.text("Passive B: ", NamedTextColor.WHITE).append(passiveb).appendNewline())
                .append(Component.text("Global: ", NamedTextColor.WHITE).append(global));
        return component;
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
            BuildSkill r1BuildSkill = this.getBuildSkill(skillType);
            BuildSkill r2BuildSkill = r2.getBuildSkill(skillType);

            if (r1BuildSkill == null && r2BuildSkill != null) {
                return false;
            }
            // still must check for null, because 2 null skills are valid
            if (r1BuildSkill != null && (!r1BuildSkill.equals(r2BuildSkill))) {
                return false;
            }
        }
        return true;
    }
}
