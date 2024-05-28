package me.mykindos.betterpvp.core.skill;

public interface ISkill {

    String getName();

    String[] getDescription(int level);

    default int getMaxLevel() {
        return 5;
    }

    boolean isEnabled();

    void loadConfig();

}
