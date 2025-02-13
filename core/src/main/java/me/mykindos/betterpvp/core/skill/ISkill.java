package me.mykindos.betterpvp.core.skill;

import net.kyori.adventure.text.Component;

public interface ISkill {

    String getName();

    String[] getDescription();

    boolean isEnabled();

    void loadConfig();

    default Component getTags() {
        return null;
    }

}
