package me.mykindos.betterpvp.clans.skills;

import me.mykindos.betterpvp.clans.skills.types.ClassType;

public interface ISkill {

    String getName();

    String getDescription(int level);

    ClassType getClassType();

    boolean isEnabled();

}
