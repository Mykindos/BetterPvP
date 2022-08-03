package me.mykindos.betterpvp.clans.champions.builds;

import lombok.Data;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;

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
    private int points;

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

}
