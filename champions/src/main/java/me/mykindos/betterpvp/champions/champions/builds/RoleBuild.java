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

    /**
     * @return The component representation of a build
     */
    public Component getBuildComponent() {
        Component sword = getSwordSkill() == null ? Component.empty() : getSwordSkill().getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + getSwordSkill().getSkill().getName().replace(" ", "_") + " " + getSwordSkill().getLevel()))
                .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to see skill description")));
        Component axe = getAxeSkill() == null ? Component.empty() : getAxeSkill().getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + getAxeSkill().getSkill().getName().replace(" ", "_") + " " + getAxeSkill().getLevel()))
                .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to see skill description")));
        Component bow = getBow() == null ? Component.empty() : getBow().getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + getBow().getSkill().getName().replace(" ", "_") + " " + getBow().getLevel()))
                .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to see skill description")));;
        Component passivea = getPassiveA() == null ? Component.empty() : getPassiveA().getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + getPassiveA().getSkill().getName().replace(" ", "_") + " " + getPassiveA().getLevel()))
                .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to see skill description")));;
        Component passiveb = getPassiveB() == null ? Component.empty() : getPassiveB().getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + getPassiveB().getSkill().getName().replace(" ", "_") + " " + getPassiveB().getLevel()))
                .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to see skill description")));;
        Component global = getGlobal() == null ? Component.empty() : getGlobal().getComponent()
                .clickEvent(ClickEvent.runCommand("/skilldescription " + getGlobal().getSkill().getName().replace(" ", "_") + " " + getGlobal().getLevel()))
                .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("<white>Click</white> to see skill description")));;

        Component component = Component.text("Sword: ", NamedTextColor.WHITE).append(sword).appendNewline()
                .append(Component.text("Axe: ", NamedTextColor.WHITE).append(axe).appendNewline())
                .append(Component.text("Bow: ", NamedTextColor.WHITE).append(bow).appendNewline())
                .append(Component.text("Passive A: ", NamedTextColor.WHITE).append(passivea).appendNewline())
                .append(Component.text("Passive B: ", NamedTextColor.WHITE).append(passiveb).appendNewline())
                .append(Component.text("Global: ", NamedTextColor.WHITE).append(global));
        return component;
    }

}
