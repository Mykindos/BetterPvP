package me.mykindos.betterpvp.core.skill;

import net.kyori.adventure.text.Component;

public interface ISkill {

    String getName();

    Component[] getDescription(int level);

    default int getMaxLevel() {
        return 5;
    }

    boolean isEnabled();

    void loadConfig();

    default Component getTags() {
        return null;
    }

}
